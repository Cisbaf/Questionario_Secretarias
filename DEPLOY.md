# Implantação com Docker Hub

## 1. Construir e enviar as imagens (no seu computador)

```bash
docker login
export U=seu_usuario_no_docker_hub  TAG=1.0.0

# Se o seu computador não for x86_64 e o servidor for, acrescente --platform linux/amd64 nos dois builds.
docker build -t $U/questionario-backend:$TAG  -t $U/questionario-backend:latest  ./QuestionarioBackEnd
docker build -t $U/questionario-frontend:$TAG -t $U/questionario-frontend:latest ./QuestionarioFrontEnd

docker push $U/questionario-backend:$TAG  && docker push $U/questionario-backend:latest
docker push $U/questionario-frontend:$TAG && docker push $U/questionario-frontend:latest
```

Não há segredo dentro das imagens: tudo vem de variáveis de ambiente na hora de subir. Se o repositório do Docker Hub
for privado, rode `docker login` também no servidor.

## 2. Preparar o MySQL do servidor (uma vez)

```sql
CREATE DATABASE questionario CHARACTER SET utf8mb4;
CREATE USER 'questionario'@'%' IDENTIFIED BY 'troque-esta-senha';
GRANT ALL PRIVILEGES ON questionario.* TO 'questionario'@'%';
```

Para os contêineres alcançarem o MySQL do servidor:
- o MySQL precisa escutar além do `127.0.0.1` (`bind-address = 0.0.0.0`, ou ao menos o IP da ponte do Docker, em geral
  `172.17.0.1`);
- o usuário precisa poder entrar vindo da rede do Docker (`'%'`, como acima, ou `'questionario'@'172.%'`);
- o firewall deve deixar a rede do Docker acessar a porta 3306 (e continuar fechada para a internet).

Se preferir, aponte `DB_HOST` para o IP/nome do servidor de banco em vez de `host.docker.internal`.

## 3. Subir no servidor

```bash
mkdir -p ~/questionario && cd ~/questionario
# copie para cá: docker-compose.example.yml (como docker-compose.yml) e .env.docker.example (como .env)
cp docker-compose.example.yml docker-compose.yml
cp .env.docker.example .env && nano .env

docker compose pull
docker compose up -d
docker compose logs -f backend      # espere "Started QuestionarioBackEndApplication"
```

Abra `http://IP_DO_SERVIDOR:3002` (ou o endereço do seu proxy). As tabelas são criadas na primeira subida.

## 4. Pontos de atenção

- **Anexos do 8.2:** os arquivos (até 3 por documento, 10 MB cada) ficam no próprio MySQL, nas tabelas `anexos` e
  `anexo_conteudo`; entram no backup do banco e não precisam de volume no Docker. Duas exigências: o `max_allowed_packet`
  do MySQL deve ser de pelo menos 16 MB (no MySQL 8 o padrão de 64 MB já serve) e, se houver proxy reverso na frente,
  libere corpos de pelo menos 12 MB (nginx: `client_max_body_size 12m;`).
- **HTTPS e login do admin:** com `COOKIE_SEGURO=true` o navegador só envia o cookie por HTTPS. Em HTTP puro, use
  `COOKIE_SEGURO=false`, ou (melhor) ponha um proxy reverso com certificado na frente do front e publique a porta como
  `127.0.0.1:3002:3002`.
- **`PUBLIC_URL`** precisa ser exatamente o endereço digitado no navegador, senão o CORS do backend recusa chamadas.
- **Atualizar versão:** `docker compose pull && docker compose up -d` (use `TAG=1.0.1` no `.env` para fixar uma versão).
- **Backup:** faça dump do banco `questionario` do MySQL do servidor; os contêineres não guardam dados.
- **Capa do questionário:** mudou uma variável `LEVANTAMENTO_*`? Edite o `.env` e rode `docker compose up -d` de novo.
