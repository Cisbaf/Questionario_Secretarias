import type { NextConfig } from "next";

// Origens (IP/host da rede local) autorizadas a abrir o servidor de DESENVOLVIMENTO por outro computador.
// Ex.: DEV_ORIGINS=192.168.1.245,meu-pc.local npm run dev   (padrão: faixas comuns de rede local)
const devOrigins = (process.env.DEV_ORIGINS ?? "")
  .split(",")
  .map((s) => s.trim())
  .filter(Boolean);

const nextConfig: NextConfig = {
  output: "standalone",
  allowedDevOrigins: ["192.168.*.*", "10.*.*.*", "172.16.*.*", ...devOrigins],
};

export default nextConfig;
