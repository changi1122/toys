package main

import "fmt"

// toggleField represents a boolean "use default value" switch that reveals
// an editable text field when turned off.
type toggleField struct {
	label        string
	defaultValue string
	useDefault   bool
}

func newToggleField(label, defaultValue string) toggleField {
	return toggleField{label: label, defaultValue: defaultValue, useDefault: true}
}

func (t *toggleField) toggle() {
	t.useDefault = !t.useDefault
}

func (t toggleField) renderRow(focused bool, inputView string) string {
	cursor := focusMarker(focused)
	if t.useDefault {
		return fmt.Sprintf("%s%s %s 기본값 사용 (%s)", cursor, successStyle.Render("[x]"), t.label, t.defaultValue)
	}
	return fmt.Sprintf("%s%s %s 기본값 사용 → 직접 입력: %s", cursor, mutedStyle.Render("[ ]"), t.label, inputView)
}
