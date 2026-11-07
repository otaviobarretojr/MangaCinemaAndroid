# Gabriel Lê — Biblioteca de Aventuras
Projeto infantil separado da aplicação MangaCinemaAndroid.

## Status
- Branch de preparação do futuro repositório independente.
- A versão PWA 1.0 já existe como pacote ZIP entregue no ChatGPT, mas **não foi publicada nem transferida integralmente para esta branch**.
- Nenhum arquivo do app original foi modificado; este diretório é isolado e não deve ser mesclado à branch main do MangaCinemaAndroid.
- O livro de demonstração abaixo pode ser importado no leitor PWA a partir do arquivo `.gbrbook`.

## Objetivo
Leitor de livros ilustrados para criança de 6 anos, com páginas independentes, ilustrações sem texto embutido, balões/legendas renderizados com HTML, progresso de leitura, biblioteca de histórias, exercícios e área de pais.

## Fluxo editorial
1. Roteiro e divisão do livro em cenas.
2. Guia fixo dos personagens (Gabriel, Luna, Theo e Floquinho).
3. Uma ilustração por página, com revisão de legibilidade e continuidade.
4. Importar imagens individualmente no editor PWA.
5. Testar em celular; exportar backup .gbrbook.
6. Publicar novas histórias e edições no catálogo.

## Próximos passos técnicos
- Importar o pacote completo do PWA 1.0 (index.html, app.js, styles.css, sw.js, manifest.webmanifest, ícones).
- Preparar GitHub Actions para validação e deploy.
- Criar um **novo repositório privado ou público dedicado** e migrar o histórico da branch, sem manter dependência do MangaCinemaAndroid.
- Definir armazenamento/sincronização entre dispositivos. O PWA 1.0 atual usa armazenamento local; a publicação de histórias não é automática nos aparelhos.
- Gerar ilustrações definitivas uma a uma e validar todas as páginas.

## Privacidade e direitos
O conteúdo é destinado a uso familiar. Não armazenar fotos pessoais da criança no repositório público. Estética voxel inspirada em jogos de blocos sem insinuar ser produto oficial da Mojang/Microsoft.
