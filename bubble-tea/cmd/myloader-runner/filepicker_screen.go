package main

import (
	"path/filepath"

	"github.com/charmbracelet/bubbles/filepicker"
	tea "github.com/charmbracelet/bubbletea"
)

type filePickerModel struct {
	picker filepicker.Model
}

func newFilePickerModel() filePickerModel {
	fp := filepicker.New()
	fp.AllowedTypes = []string{".sql", ".sql.gz"}
	fp.DirAllowed = true
	fp.FileAllowed = true
	fp.SetHeight(15)
	return filePickerModel{picker: fp}
}

func (fm filePickerModel) initCmd() tea.Cmd {
	return fm.picker.Init()
}

func (m model) updateFilePicker(msg tea.Msg) (tea.Model, tea.Cmd) {
	if keyMsg, ok := msg.(tea.KeyMsg); ok && keyMsg.String() == "q" {
		return m, tea.Quit
	}

	var cmd tea.Cmd
	m.filepicker.picker, cmd = m.filepicker.picker.Update(msg)

	if didSelect, path := m.filepicker.picker.DidSelectFile(msg); didSelect {
		m.cfg.backupDir = filepath.Dir(path)
		m.current = screenConfirm
		return m, cmd
	}

	return m, cmd
}

func (m model) viewFilePicker() string {
	body := mutedStyle.Render("덤프 폴더 안의 .sql 또는 .sql.gz 파일을 하나 선택하세요 (q: 종료)") +
		"\n\n" + m.filepicker.picker.View()
	return renderScreen("백업 파일 선택", body)
}
