package main

import (
	"bufio"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/charmbracelet/bubbles/stopwatch"
	"github.com/charmbracelet/bubbles/viewport"
	"github.com/charmbracelet/lipgloss"
	tea "github.com/charmbracelet/bubbletea"
)

// execLineMsg carries one line of combined stdout/stderr output.
type execLineMsg string

// execDoneMsg is sent once when the command has exited (or failed to start).
type execDoneMsg struct {
	err      error
	exitCode int
}

// execStartedMsg hands the channels to the model so Update can start
// listening on them.
type execStartedMsg struct {
	lineCh chan execLineMsg
	doneCh chan execDoneMsg
}

type execModel struct {
	viewport   viewport.Model
	stopwatch  stopwatch.Model
	lines      []string
	lineCh     chan execLineMsg
	doneCh     chan execDoneMsg
	finished   bool
	execErr    error
	exitCode   int
	autoscroll bool
}

// chromeHeight/chromeWidth reserve room for the title, status/timer line,
// box border+padding, and footer around the log viewport.
const (
	chromeHeight = 9
	chromeWidth  = 6
)

func newExecModel(width, height int) execModel {
	vp := viewport.New(viewportSize(width, height))
	return execModel{viewport: vp, stopwatch: stopwatch.New(), autoscroll: true}
}

func viewportSize(width, height int) (int, int) {
	w, h := width-chromeWidth, height-chromeHeight
	if w < 20 {
		w = 20
	}
	if h < 5 {
		h = 5
	}
	return w, h
}

// resize adjusts the log viewport to the terminal size and re-wraps
// already-buffered lines so nothing is cut off. Safe to call before the
// exec screen has ever been shown (it just resizes a not-yet-used model).
func (e *execModel) resize(width, height int) {
	e.viewport.Width, e.viewport.Height = viewportSize(width, height)
	e.refreshViewportContent()
}

// refreshViewportContent re-wraps every buffered line to the viewport's
// current width and rebuilds the viewport content. viewport.SetContent has
// no incremental-append, and raw long lines are never wrapped on their own,
// so this must run whenever a line is added or the width changes.
func (e *execModel) refreshViewportContent() {
	w := e.viewport.Width
	if w <= 0 {
		w = 80
	}
	wrap := lipgloss.NewStyle().Width(w)
	wrapped := make([]string, len(e.lines))
	for i, line := range e.lines {
		wrapped[i] = wrap.Render(line)
	}
	e.viewport.SetContent(strings.Join(wrapped, "\n"))
	if e.autoscroll {
		e.viewport.GotoBottom()
	}
}

// ensureMetadataFile creates an empty "metadata" file in dir if one isn't
// already there. myloader (1.0.3-1) requires the file to at least contain a
// [config] section - a fully empty file fails with "Section [config] was
// not found on metadata file".
func ensureMetadataFile(dir string) error {
	path := filepath.Join(dir, "metadata")
	if _, err := os.Stat(path); err == nil {
		return nil
	} else if !os.IsNotExist(err) {
		return err
	}
	return os.WriteFile(path, []byte("[config]\n"), 0o644)
}

// runMyloader starts myloader with the given config, wires two goroutines
// (stdout+stderr) that scan lines into a shared channel, and a third
// goroutine that waits for the process to exit and reports the result.
func runMyloader(cfg config) tea.Cmd {
	return func() tea.Msg {
		if err := ensureMetadataFile(cfg.backupDir); err != nil {
			return execDoneMsg{err: fmt.Errorf("metadata 파일 생성 실패: %w", err), exitCode: -1}
		}

		cmd := exec.Command("myloader",
			"--host="+cfg.dbHost,
			"--port="+cfg.dbPort,
			"--database="+cfg.dbName,
			"--directory="+cfg.backupDir,
			"--user="+cfg.dbUser,
			"--password="+cfg.password,
			"--threads="+cfg.threads,
			"-o",
			"--verbose=3",
		)

		stdout, err := cmd.StdoutPipe()
		if err != nil {
			return execDoneMsg{err: err, exitCode: -1}
		}
		stderr, err := cmd.StderrPipe()
		if err != nil {
			return execDoneMsg{err: err, exitCode: -1}
		}

		if err := cmd.Start(); err != nil {
			// e.g. "executable file not found in $PATH" when myloader is missing
			return execDoneMsg{err: err, exitCode: -1}
		}

		lineCh := make(chan execLineMsg, 256)
		doneCh := make(chan execDoneMsg, 1)

		var wg sync.WaitGroup
		wg.Add(2)
		scan := func(r io.Reader) {
			defer wg.Done()
			sc := bufio.NewScanner(r)
			sc.Buffer(make([]byte, 64*1024), 1024*1024) // verbose=3 lines can be long
			for sc.Scan() {
				lineCh <- execLineMsg(sc.Text())
			}
		}
		go scan(stdout)
		go scan(stderr)

		go func() {
			wg.Wait() // both pipes fully drained
			waitErr := cmd.Wait()
			close(lineCh) // signals "no more lines" to waitForLine
			code := 0
			if exitErr, ok := waitErr.(*exec.ExitError); ok {
				code = exitErr.ExitCode()
			} else if waitErr != nil {
				code = -1
			}
			doneCh <- execDoneMsg{err: waitErr, exitCode: code}
		}()

		return execStartedMsg{lineCh: lineCh, doneCh: doneCh}
	}
}

// waitForLine must be re-issued from Update after every execLineMsg to keep
// listening - bubbletea has no "subscribe to a channel forever" primitive.
func waitForLine(ch chan execLineMsg) tea.Cmd {
	return func() tea.Msg {
		line, ok := <-ch
		if !ok {
			return nil
		}
		return line
	}
}

func waitForDone(ch chan execDoneMsg) tea.Cmd {
	return func() tea.Msg {
		return <-ch
	}
}

func formatElapsed(d time.Duration) string {
	d = d.Round(time.Second)
	h := d / time.Hour
	d -= h * time.Hour
	m := d / time.Minute
	d -= m * time.Minute
	s := d / time.Second
	if h > 0 {
		return fmt.Sprintf("%02d:%02d:%02d", h, m, s)
	}
	return fmt.Sprintf("%02d:%02d", m, s)
}

func (m model) updateExec(msg tea.Msg) (tea.Model, tea.Cmd) {
	var swCmd tea.Cmd
	m.execModel.stopwatch, swCmd = m.execModel.stopwatch.Update(msg)

	switch msg := msg.(type) {
	case execStartedMsg:
		m.execModel.lineCh = msg.lineCh
		m.execModel.doneCh = msg.doneCh
		return m, tea.Batch(swCmd, waitForLine(msg.lineCh), waitForDone(msg.doneCh))

	case execLineMsg:
		m.execModel.lines = append(m.execModel.lines, string(msg))
		m.execModel.refreshViewportContent()
		return m, tea.Batch(swCmd, waitForLine(m.execModel.lineCh))

	case execDoneMsg:
		m.execModel.finished = true
		m.execModel.execErr = msg.err
		m.execModel.exitCode = msg.exitCode
		m.current = screenDone
		return m, tea.Batch(swCmd, m.execModel.stopwatch.Stop())

	case tea.KeyMsg:
		if m.execModel.finished {
			return m, tea.Quit
		}
		switch msg.String() {
		case "up", "k":
			m.execModel.autoscroll = false
			var cmd tea.Cmd
			m.execModel.viewport, cmd = m.execModel.viewport.Update(msg)
			return m, tea.Batch(swCmd, cmd)
		case "down", "j":
			var cmd tea.Cmd
			m.execModel.viewport, cmd = m.execModel.viewport.Update(msg)
			if m.execModel.viewport.AtBottom() {
				m.execModel.autoscroll = true
			}
			return m, tea.Batch(swCmd, cmd)
		}
	}
	return m, swCmd
}

func (m model) viewExec() string {
	var status string
	switch {
	case m.current == screenExecuting:
		status = focusedStyle.Render("⏳ myloader 실행 중...")
	case m.execModel.execErr == nil:
		status = successStyle.Render("✔ 실행 완료 (exit code 0)")
	default:
		status = errorStyle.Render(fmt.Sprintf("✘ 실행 실패: %v (exit code %d)", m.execModel.execErr, m.execModel.exitCode))
	}
	elapsed := mutedStyle.Render("⏱ 경과 시간 " + formatElapsed(m.execModel.stopwatch.Elapsed()))

	var b strings.Builder
	b.WriteString(status + "   " + elapsed + "\n\n")
	b.WriteString(m.execModel.viewport.View())
	if m.current == screenDone {
		b.WriteString("\n\n" + mutedStyle.Render("아무 키나 눌러 종료"))
	} else {
		b.WriteString("\n\n" + mutedStyle.Render("완료될 때까지 종료 불가 (↑/↓, j/k: 스크롤)"))
	}
	return renderScreen("myloader 실행 로그", b.String())
}
