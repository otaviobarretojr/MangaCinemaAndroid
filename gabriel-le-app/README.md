# Gabriel Lê — Biblioteca de Aventuras

Leitor de histórias infantis em português, criado para uso familiar. Projeto **PWA**: instala-se na tela inicial do Android pelo Chrome após abrir o site HTTPS.

## Acessos

- Aplicativo publicado: https://gabriel-le-aventuras.onrender.com/
- Render: https://dashboard.render.com/static/srv-dbnn8ek9v7es73d83a2g
- Código-fonte: branch `feature/gabriel-le-biblioteca`, pasta `gabriel-le-app/` (não mesclar à main).

## Status real — 07/11/2026

- PWA completo na pasta atual (`index.html`, `styles.css`, `app.js`, `sw.js`, manifesto e ícones).
- Serviço estático Render implantado; deploy inicial reportado como `live`.
- Testes automatizados locais: abertura da biblioteca, navegação, edição, upload de imagem, inclusão/importação de livro e quiz, sem erros JavaScript. **Ainda falta validar em Android físico.**
- Livro de demonstração: `books/gabriel-e-o-portal-misterioso.gbrbook`, com 19 páginas de texto, quiz e **ilustrações definitivas pendentes**.
- O Render acompanha automaticamente commits na branch `feature/gabriel-le-biblioteca`.

## Como usar

1. Abra o site no Chrome do Android.
2. No menu do navegador, escolha **Instalar app** ou **Adicionar à tela inicial**, quando disponível.
3. Abra o livro de demonstração na biblioteca.
4. Para acrescentar ilustrações: **Área dos pais > Editar > Importar ilustração desta página**.
5. Antes de trocar de telefone, exporte um **backup** da biblioteca.

## Arquitetura e limites

- HTML/CSS/JavaScript sem backend; app roda offline depois de carregar todos os assets e registrar o service worker.
- Livros, imagens importadas e posição de leitura ficam no **IndexedDB do dispositivo**: sem sincronização automática entre aparelhos.
- O site público entrega o *código* e o livro de demonstração embutido, mas alterações feitas na Área dos pais **não aparecem nos outros aparelhos nem são enviadas para o GitHub**.
- Ao apagar dados do navegador, há risco de perder a biblioteca local; use a exportação.
- Sem conta, anúncios ou analytics. Área dos pais ainda **não tem bloqueio de acesso**; a segurança física do dispositivo continua importante.
- Para novas histórias chegarem automaticamente a todos os aparelhos, é necessária uma próxima versão com catálogo remoto versionado e política de atualização.

## Processo editorial

1. Escrever roteiro e dividir em páginas.
2. Preparar guia visual dos personagens Gabriel, Luna, Theo e Floquinho.
3. Criar **uma arte por página**, sem texto, garantindo consistência e margem de segurança.
4. Compor texto e balões pelo app (não dentro da ilustração).
5. Revisar em celular: cortes, legibilidade, continuidade, navegabilidade e quiz.
6. Exportar o livro `.gbrbook` e manter cópias de segurança.

## Futuro repositório isolado

O código está numa branch separada do `MangaCinemaAndroid`, mas ainda dentro do **repositório público** dele. Planejado: criar `Gabriel-Aventuras` como repositório independente, preferencialmente **privado**, mover esta pasta para a raiz, ajustar o Render e então retirar a branch temporária. Não salvar fotos privadas da criança em repositório público.

Projeto familiar inspirado em universos de blocos, não oficial da Mojang/Microsoft.

## Edição ilustrada v1.2 — publicada em 07/11/2026

- 19 ilustrações individuais: páginas p01, p02, p04–p19 em WebP e p03 em JPEG.
- Arquivos permanentes em `assets/portal/`; ilustrações e texto permanecem separados para não cortar frases.
- `app.js` usa `OFFICIAL_ART` para exibir as novas imagens **mesmo em livros antigos já salvos no aparelho**, sem apagar títulos/textos locais nem substituir imagens personalizadas.
- Página 18 agora mostra ilustração própria junto das perguntas interativas.
- Imagens são pré-armazenadas para uso offline pela nova versão do service worker.
- `books/catalog.json` permite publicar títulos **novos** que surgem na biblioteca quando houver conexão. Obras do usuário com o mesmo ID não são substituídas automaticamente.
- A página foi adaptada para a proporção vertical das cenas, com imagem inteira e texto abaixo.
- O site continua um PWA instalável, não APK; não houve geração de vídeo pago.
- A equipe familiar pode criar ou importar outras histórias na Área dos Pais. Os arquivos editados localmente permanecem no aparelho, salvo backup manual.
- Validação: 19 imagens presentes no repositório, importação automatizada via GitHub Actions bem-sucedida, último deploy Render confirmado como `live`. Teste em aparelho físico ainda recomendado.

### Novas histórias

Criar um arquivo `.gbrbook` com novo `id` na pasta `books/`, adicionar as ilustrações permanentes em `assets/`, incluir a entrada no `books/catalog.json` e publicar. Não é necessário reconstruir o aplicativo.
