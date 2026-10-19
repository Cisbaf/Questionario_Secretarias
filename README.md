# Questionário às Secretarias Municipais de Saúde (CISBAF)

Versão web do documento "Questionário às Secretarias Municipais de Saúde — Prioridades de atenção à saúde, filas de
regulação e pacientes sem vaga nas portas de urgência". O plano completo, as decisões e o inventário de campos estão em
[`PLANO_QUESTIONARIO_GUIADO.md`](PLANO_QUESTIONARIO_GUIADO.md).

- `QuestionarioBackEnd/`: Java 21, Spring Boot 4, JPA/MySQL, Springdoc. Porta **8082**.
- `QuestionarioFrontEnd/`: Next.js 16 (App Router), TypeScript, MUI. Porta **3002**. O navegador só fala com o Next
  (`/api/...`), que repassa ao Spring (BFF).

## Rodar em desenvolvimento

1. MySQL: use o do `docker-compose.yml` da raiz do IdeaProjects (`docker compose up -d mysql`, porta 3307). O banco
   `questionario` é criado sozinho.
2. Backend (JDK 21, sem `-q`):

   ```bash
   cd QuestionarioBackEnd
   mvn spring-boot:run
   mvn test
   ```

3. Frontend (Node 22):

   ```bash
   cd QuestionarioFrontEnd
   cp .env.example .env
   npm install
   npm run dev
   ```

Abra `http://localhost:3002`. Swagger (só em dev): `http://localhost:8082/swagger-ui/index.html`.
Saúde da API: `http://localhost:8082/actuator/health`.

## Capa do questionário (levantamento)

Período de referência, data do censo, prazo de devolução e e-mail de envio vêm das variáveis `LEVANTAMENTO_*` (veja o
`.env.example`), com datas em `AAAA-MM-DD`. O backend as grava no banco **a cada subida**; para mudar a capa, altere a
variável e reinicie. Variável em branco aparece como "a definir". Para o `mvn spring-boot:run` local, exporte-as no
terminal ou no IntelliJ (Run > Edit Configurations > Environment variables).

Não há login no questionário. A área do admin (login, visualização, Excel) é a etapa E11 do plano.
