package main

import (
	"fmt"
	"strings"

	tea "github.com/charmbracelet/bubbletea"
)

func (m model) updateConfirm(msg tea.Msg) (tea.Model, tea.Cmd) {
	if keyMsg, ok := msg.(tea.KeyMsg); ok {
		switch keyMsg.String() {
		case "enter":
			m.current = screenExecuting
			m.execModel = newExecModel(m.width, m.height)
			return m, tea.Batch(runMyloader(m.cfg), m.execModel.stopwatch.Init())
		case "q":
			return m, tea.Quit
		}
	}
	return m, nil
}

func (m model) viewConfirm() string {
	c := m.cfg
	label := mutedStyle.Render
	var b strings.Builder
	b.WriteString(" 다음 설정으로 로드를 시작합니다:\n\n")
	b.WriteString(fmt.Sprintf(" %s : %s\n", label("대상 호스트      "), c.dbHost))
	b.WriteString(fmt.Sprintf(" %s : %s\n", label("대상 포트        "), c.dbPort))
	b.WriteString(fmt.Sprintf(" %s : %s\n", label("대상 데이터베이스"), c.dbName))
	b.WriteString(fmt.Sprintf(" %s : %s\n", label("대상 사용자      "), c.dbUser))
	b.WriteString(fmt.Sprintf(" %s : %s\n", label("백업 디렉토리    "), c.backupDir))
	b.WriteString(fmt.Sprintf(" %s : %s\n", label("실행 스레드 수   "), c.threads))
	b.WriteString(fmt.Sprintf(" %s : %s\n", label("비밀번호         "), mutedStyle.Render("***")))
	b.WriteString("\n" + mutedStyle.Render("enter: 실행 시작, esc/q: 종료"))
	return renderScreen("실행 전 확인", b.String())
}
