# Instruções obrigatórias para agentes

Antes de analisar ou alterar este repositório, leia as instruções compartilhadas em `../nord-center-infra-agent`:

1. `AGENTS.md` — regras gerais para agentes.
2. `projects/nord-tool-backend.md` — perfil e contexto deste projeto.
3. Todos os arquivos Markdown em `policies/` — regras globais de segurança, permissões, branches e fluxo Git.
4. Todos os arquivos Markdown em `steering/nord-tool-backend/` — regras permanentes deste projeto.
5. A skill aplicável à tarefa em `skills/nord-tool-backend/`. Se a tarefa envolver mais de uma área, leia todas as skills pertinentes antes de agir.

Não comece alterações até concluir essa leitura. Se o diretório compartilhado não estiver disponível, ou se houver instruções ausentes ou conflitantes, informe o impedimento e não improvise regras locais.

Se usar o MCP, chame `get_project_context` para este projeto antes de qualquer operação de escrita e inclua o `context_token` retornado na chamada de escrita.

Depois da leitura, confira o código e a configuração atuais, em especial `pom.xml`, antes de implementar. Preserve Java 11 e Spring Boot 2.7.x, salvo pedido explícito de migração. Nunca exponha segredos. Para tarefas de Git, siga as policies compartilhadas.
