"use client";

import { createTheme } from "@mui/material/styles";

export const theme = createTheme({
  palette: {
    mode: "light",
    primary: { main: "#1F6F50", dark: "#154D38", light: "#E4F0EA" },
    secondary: { main: "#F5B700" },
    background: { default: "#F5F4EF", paper: "#FFFFFF" },
  },
  typography: {
    fontFamily: "Inter, Arial, sans-serif",
    h4: { fontWeight: 750, letterSpacing: "-0.03em" },
    h6: { fontWeight: 700 },
    button: { fontWeight: 700, textTransform: "none" },
  },
  shape: { borderRadius: 12 },
  components: {
    MuiPaper: { styleOverrides: { root: { backgroundImage: "none" } } },
    MuiButton: { defaultProps: { disableElevation: true } },
    MuiTableCell: { styleOverrides: { head: { fontWeight: 700, color: "#526071", background: "#F8FAFC" } } },
  },
});
