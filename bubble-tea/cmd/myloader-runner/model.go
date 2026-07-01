package main

import (
	"os/exec"

	tea "github.com/charmbracelet/bubbletea"
)

type screen int

const (
	screenMyloaderWarning screen = iota
	screenForm
	screenFilePicker
	screenConfirm
	screenExecuting
	screenDone
)

type config struct {
	dbName    string
	dbUser    string
	password  string
	dbHost    string
	dbPort    string
	threads   string
	backupDir string
}

type model struct {
	current         screen
	cfg             config
	myloaderMissing bool
	width, height   int

	form       formModel
	filepicker filePickerModel
	execModel  execModel
}

func newModel() model {
	_, err := exec.LookPath("myloader")
	missing := err != nil

	m := model{
		myloaderMissing: missing,
		form:            newFormModel("127.0.0.1"),
	}
	if missing {
		m.current = screenMyloaderWarning
	} else {
		m.current = screenForm
	}
	return m
}

func (m model) Init() tea.Cmd {
	if m.current == screenForm {
		return m.form.initCmd()
	}
	return nil
}

func (m model) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	if keyMsg, ok := msg.(tea.KeyMsg); ok {
		switch keyMsg.String() {
		case "ctrl+c", "esc":
			if m.current == screenExecuting && !m.execModel.finished {
				return m, nil
			}
			return m, tea.Quit
		}
	}

	if sizeMsg, ok := msg.(tea.WindowSizeMsg); ok {
		m.width, m.height = sizeMsg.Width, sizeMsg.Height
		m.execModel.resize(m.width, m.height)
	}

	switch m.current {
	case screenMyloaderWarning:
		return m.updateWarning(msg)
	case screenForm:
		return m.updateForm(msg)
	case screenFilePicker:
		return m.updateFilePicker(msg)
	case screenConfirm:
		return m.updateConfirm(msg)
	case screenExecuting, screenDone:
		return m.updateExec(msg)
	}
	return m, nil
}

func (m model) View() string {
	switch m.current {
	case screenMyloaderWarning:
		return m.viewWarning()
	case screenForm:
		return m.viewForm()
	case screenFilePicker:
		return m.viewFilePicker()
	case screenConfirm:
		return m.viewConfirm()
	case screenExecuting, screenDone:
		return m.viewExec()
	}
	return ""
}
