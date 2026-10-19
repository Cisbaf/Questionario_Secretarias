import { Box, Container, Typography } from "@mui/material";

export default function Cabecalho() {
  return (
    <Box component="header" sx={{ bgcolor: "background.paper", borderBottom: "1px solid", borderColor: "divider" }}>
      <Container maxWidth="lg" sx={{ display: "flex", alignItems: "center", gap: 2, py: 1.5 }}>
        <Box component="img" src="/cisbaf.png" alt="CISBAF" sx={{ height: 48, width: 48, objectFit: "contain" }} />
        <Box>
          <Typography variant="subtitle1" sx={{ fontWeight: 700, lineHeight: 1.2 }}>
            Consórcio Intermunicipal de Saúde da Baixada Fluminense
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Observatório Regional de Saúde | Equipe de Planejamento Regional
          </Typography>
        </Box>
      </Container>
    </Box>
  );
}
