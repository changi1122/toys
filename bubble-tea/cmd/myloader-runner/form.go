package main

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/charmbracelet/bubbles/textinput"
	tea "github.com/charmbracelet/bubbletea"
)

type formFieldKind int

const (
	fieldDBNameToggle formFieldKind = iota
	fieldDBNameInput
	fieldDBUserToggle
	fieldDBUserInput
	fieldPassword
	fieldDBHost
	fieldDBPort
	fieldThreads
)

type formModel struct {
	dbNameToggle toggleField
	dbNameInput  textinput.Model
	dbUserToggle toggleField
	dbUserInput  textinput.Model
	password     textinput.Model
	dbHost       textinput.Model
	dbPort       textinput.Model
	threads      textinput.Model

	focusOrder []formFieldKind
	focusIdx   int
	errMsg     string
}

func newFormModel(defaultHost string) formModel {
	dbNameInput := textinput.New()
	dbNameInput.SetValue("dqlite")
	dbNameInput.CharLimit = 64
	dbNameInput.Width = 30

	dbUserInput := textinput.New()
	dbUserInput.SetValue("root")
	dbUserInput.CharLimit = 64
	dbUserInput.Width = 30

	password := textinput.New()
	password.Placeholder = "비밀번호 입력"
	password.EchoMode = textinput.EchoPassword
	password.EchoCharacter = '*'
	password.CharLimit = 128
	password.Width = 30

	dbHost := textinput.New()
	dbHost.SetValue(defaultHost)
	dbHost.CharLimit = 64
	dbHost.Width = 30

	dbPort := textinput.New()
	dbPort.SetValue("43396")
	dbPort.CharLimit = 5
	dbPort.Width = 30

	threads := textinput.New()
	threads.SetValue("4")
	threads.CharLimit = 3
	threads.Width = 30

	f := formModel{
		dbNameToggle: newToggleField("DB_NAME", "dqlite"),
		dbNameInput:  dbNameInput,
		dbUserToggle: newToggleField("DB_USER", "root"),
		dbUserInput:  dbUserInput,
		password:     password,
		dbHost:       dbHost,
		dbPort:       dbPort,
		threads:      threads,
	}
	f.rebuildFocusOrder()
	return f
}

func (f *formModel) rebuildFocusOrder() {
	order := []formFieldKind{fieldDBNameToggle}
	if !f.dbNameToggle.useDefault {
		order = append(order, fieldDBNameInput)
	}
	order = append(order, fieldDBUserToggle)
	if !f.dbUserToggle.useDefault {
		order = append(order, fieldDBUserInput)
	}
	order = append(order, fieldPassword, fieldDBHost, fieldDBPort, fieldThreads)
	f.focusOrder = order
	if f.focusIdx >= len(f.focusOrder) {
		f.focusIdx = len(f.focusOrder) - 1
	}
	if f.focusIdx < 0 {
		f.focusIdx = 0
	}
}

func (f *formModel) currentKind() formFieldKind {
	if f.focusIdx < 0 || f.focusIdx >= len(f.focusOrder) {
		return fieldDBNameToggle
	}
	return f.focusOrder[f.focusIdx]
}

func (f *formModel) inputFor(kind formFieldKind) *textinput.Model {
	switch kind {
	case fieldDBNameInput:
		return &f.dbNameInput
	case fieldDBUserInput:
		return &f.dbUserInput
	case fieldPassword:
		return &f.password
	case fieldDBHost:
		return &f.dbHost
	case fieldDBPort:
		return &f.dbPort
	case fieldThreads:
		return &f.threads
	default:
		return nil
	}
}

func (f *formModel) focusCurrent() tea.Cmd {
	ti := f.inputFor(f.currentKind())
	if ti == nil {
		return nil
	}
	return ti.Focus()
}

func (f *formModel) blurCurrent() {
	ti := f.inputFor(f.currentKind())
	if ti != nil {
		ti.Blur()
	}
}

func (f *formModel) updateFocusedInput(msg tea.Msg) tea.Cmd {
	ti := f.inputFor(f.currentKind())
	if ti == nil {
		return nil
	}
	var cmd tea.Cmd
	*ti, cmd = ti.Update(msg)
	return cmd
}

func (f *formModel) initCmd() tea.Cmd {
	return f.focusCurrent()
}

func (f *formModel) validate() error {
	if strings.TrimSpace(f.dbHost.Value()) == "" {
		return fmt.Errorf("DB_HOST를 입력해주세요")
	}
	if _, err := strconv.Atoi(strings.TrimSpace(f.dbPort.Value())); err != nil {
		return fmt.Errorf("DB_PORT는 숫자로 입력해주세요")
	}
	if _, err := strconv.Atoi(strings.TrimSpace(f.threads.Value())); err != nil {
		return fmt.Errorf("THREADS는 숫자로 입력해주세요")
	}
	if strings.TrimSpace(f.password.Value()) == "" {
		return fmt.Errorf("PASSWORD를 입력해주세요")
	}
	return nil
}

func (f *formModel) toConfig() config {
	cfg := config{
		dbName:   f.dbNameToggle.defaultValue,
		dbUser:   f.dbUserToggle.defaultValue,
		password: f.password.Value(),
		dbHost:   f.dbHost.Value(),
		dbPort:   f.dbPort.Value(),
		threads:  f.threads.Value(),
	}
	if !f.dbNameToggle.useDefault {
		cfg.dbName = f.dbNameInput.Value()
	}
	if !f.dbUserToggle.useDefault {
		cfg.dbUser = f.dbUserInput.Value()
	}
	return cfg
}

func (m model) updateWarning(msg tea.Msg) (tea.Model, tea.Cmd) {
	if _, ok := msg.(tea.KeyMsg); ok {
		m.current = screenForm
		return m, m.form.initCmd()
	}
	return m, nil
}

func (m model) viewWarning() string {
	body := errorStyle.Render("⚠ myloader가 설치되어 있지 않습니다.") + "\n\n" +
		"  mydumper 패키지를 설치해주세요 (myloader는 mydumper 패키지에 포함되어 있습니다):\n" +
		"    " + focusedStyle.Render("sudo apt install mydumper") + "\n" +
		"  (배포판 저장소 버전이 오래된 경우 https://github.com/mydumper/mydumper/releases 참고)\n\n" +
		mutedStyle.Render("설정은 계속 진행할 수 있지만, 실행 단계에서는 실패합니다.") + "\n\n" +
		mutedStyle.Render("아무 키나 눌러 계속... (esc: 종료)")
	return renderScreen("설치 확인", body)
}

func (m model) updateForm(msg tea.Msg) (tea.Model, tea.Cmd) {
	f := &m.form

	if keyMsg, ok := msg.(tea.KeyMsg); ok {
		kind := f.currentKind()
		switch keyMsg.String() {
		case "tab", "down":
			f.blurCurrent()
			f.focusIdx = (f.focusIdx + 1) % len(f.focusOrder)
			cmd := f.focusCurrent()
			return m, cmd
		case "shift+tab", "up":
			f.blurCurrent()
			f.focusIdx--
			if f.focusIdx < 0 {
				f.focusIdx = len(f.focusOrder) - 1
			}
			cmd := f.focusCurrent()
			return m, cmd
		case " ":
			if kind == fieldDBNameToggle {
				f.dbNameToggle.toggle()
				if f.dbNameToggle.useDefault {
					f.dbNameInput.SetValue(f.dbNameToggle.defaultValue)
				}
				f.rebuildFocusOrder()
				return m, nil
			}
			if kind == fieldDBUserToggle {
				f.dbUserToggle.toggle()
				if f.dbUserToggle.useDefault {
					f.dbUserInput.SetValue(f.dbUserToggle.defaultValue)
				}
				f.rebuildFocusOrder()
				return m, nil
			}
		case "enter":
			if kind == fieldThreads {
				if err := f.validate(); err != nil {
					f.errMsg = err.Error()
					return m, nil
				}
				f.errMsg = ""
				m.cfg = f.toConfig()
				m.filepicker = newFilePickerModel()
				m.current = screenFilePicker
				return m, m.filepicker.initCmd()
			}
			f.blurCurrent()
			f.focusIdx = (f.focusIdx + 1) % len(f.focusOrder)
			cmd := f.focusCurrent()
			return m, cmd
		}
	}

	cmd := f.updateFocusedInput(msg)
	return m, cmd
}

func (m model) viewForm() string {
	f := m.form
	cur := f.currentKind()

	var b strings.Builder
	b.WriteString(mutedStyle.Render("Tab/Shift+Tab: 이동, Space: 토글, Enter: 다음") + "\n\n")

	b.WriteString(f.dbNameToggle.renderRow(cur == fieldDBNameToggle || cur == fieldDBNameInput, f.dbNameInput.View()) + "\n")
	b.WriteString(f.dbUserToggle.renderRow(cur == fieldDBUserToggle || cur == fieldDBUserInput, f.dbUserInput.View()) + "\n")
	b.WriteString(focusMarker(cur == fieldPassword) + "PASSWORD : " + f.password.View() + "\n")
	b.WriteString(focusMarker(cur == fieldDBHost) + "DB_HOST  : " + f.dbHost.View() + "\n")
	b.WriteString(focusMarker(cur == fieldDBPort) + "DB_PORT  : " + f.dbPort.View() + "\n")
	b.WriteString(focusMarker(cur == fieldThreads) + "THREADS  : " + f.threads.View() + "\n")

	if f.errMsg != "" {
		b.WriteString("\n" + errorStyle.Render("⚠ "+f.errMsg) + "\n")
	}
	b.WriteString("\n" + mutedStyle.Render("esc: 종료"))
	return renderScreen("myloader 설정", b.String())
}
