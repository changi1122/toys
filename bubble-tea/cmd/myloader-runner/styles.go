package main

import "github.com/charmbracelet/lipgloss"

var (
	colorAccent  = lipgloss.Color("39")  // cyan
	colorSuccess = lipgloss.Color("42")  // green
	colorError   = lipgloss.Color("203") // red
	colorMuted   = lipgloss.Color("240") // gray

	titleStyle = lipgloss.NewStyle().
			Bold(true).
			Foreground(lipgloss.Color("230")).
			Background(colorAccent).
			Padding(0, 1)

	boxStyle = lipgloss.NewStyle().
			Border(lipgloss.RoundedBorder()).
			BorderForeground(colorAccent).
			Padding(0, 1)

	successStyle = lipgloss.NewStyle().Bold(true).Foreground(colorSuccess)
	errorStyle   = lipgloss.NewStyle().Bold(true).Foreground(colorError)
	mutedStyle   = lipgloss.NewStyle().Foreground(colorMuted)
	focusedStyle = lipgloss.NewStyle().Bold(true).Foreground(colorAccent)
)

// renderScreen wraps a screen's body in a titled, bordered box so every
// screen shares the same look.
func renderScreen(title, body string) string {
	return "\n" + titleStyle.Render(" "+title+" ") + "\n" + boxStyle.Render(body) + "\n"
}

// focusMarker returns a colored arrow for focused rows, or a blank prefix
// of the same width otherwise.
func focusMarker(focused bool) string {
	if focused {
		return focusedStyle.Render("▶ ")
	}
	return "  "
}
