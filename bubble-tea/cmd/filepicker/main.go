package main

import (
	"fmt"
	"os"

	"github.com/charmbracelet/bubbles/filepicker"
	tea "github.com/charmbracelet/bubbletea"
)

type model struct {
	filepicker   filepicker.Model
	selectedFile string
	quitting     bool
	err          error
}

func newModel() model {
	fp := filepicker.New()
	fp.CurrentDirectory, _ = os.Getwd() // 시작 위치를 절대경로로 명확히
	fp.DirAllowed = true                // New()의 기본값은 false라 반드시 켜야 폴더 진입 가능
	fp.FileAllowed = true
	fp.SetHeight(15) // AutoHeight/WindowSizeMsg 처리를 생략하기 위해 고정 높이 사용

	return model{filepicker: fp}
}

func (m model) Init() tea.Cmd {
	return m.filepicker.Init()
}

func (m model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	var cmd tea.Cmd

	if keyMsg, ok := msg.(tea.KeyMsg); ok {
		switch keyMsg.String() {
		case "q", "ctrl+c", "esc":
			m.quitting = true
			return m, tea.Quit
		}
	}

	m.filepicker, cmd = m.filepicker.Update(msg)

	if didSelect, path := m.filepicker.DidSelectFile(msg); didSelect {
		m.selectedFile = path
		m.quitting = true
		return m, tea.Quit
	}

	if didSelect, path := m.filepicker.DidSelectDisabledFile(msg); didSelect {
		m.err = fmt.Errorf("%q is not valid", path)
	}

	return m, cmd
}

func (m model) View() string {
	if m.quitting && m.selectedFile != "" {
		return fmt.Sprintf("Selected file: %s\n", m.selectedFile)
	}
	if m.quitting {
		return "No file selected.\n"
	}
	return "\nPick a file (q to quit):\n\n" + m.filepicker.View()
}

func main() {
	if _, err := tea.NewProgram(newModel()).Run(); err != nil {
		fmt.Println("Error running program:", err)
		os.Exit(1)
	}
}
