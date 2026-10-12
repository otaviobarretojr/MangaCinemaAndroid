# Mangá Cinema — Android V0.2.0

Um app Android para **importar um PDF e navegar por cenas como motion comic** ou **exportar as mesmas cenas como vídeo H.264/MP4**. Offline, sem conta, anúncios, servidores, bibliotecas nativas de terceiros ou custo de processamento remoto. A arte do PDF não é incorporada no APK.

## Dois modos

### 1) Leitor cinematográfico
1. Toque em **Importar PDF** e selecione o arquivo pela interface de arquivos do Android.
2. Aguarde a varredura das páginas. O detector procura faixas brancas entre quadros e limita a altura das cenas muito longas.
3. Cenas são exibidas com pan/zoom sutil e dissolução de 440 ms.
4. Toque nas bordas para voltar/avançar, no centro para pausar ou utilize os botões e a velocidade 0,75×/1×/1,25×/1,5×.
5. O app lembra a URI do último PDF e pede que o Android preserve a permissão de leitura.

### 2) Exportação MP4
1. Importe um PDF e aguarde terminar a análise.
2. Escolha **Rápido 480p** (480×848, 12 fps) ou **HD 720p** (720×1280, 15 fps).
3. Toque em **Exportar MP4** e escolha nome/pasta no seletor de documentos do Android.
4. Mantenha o aplicativo aberto e acompanhe o indicador de progresso. É possível tocar em **Cancelar MP4**.
5. Ao terminar, abra o MP4 salvo na pasta escolhida e compartilhe-o pelo gerenciador de arquivos.

**Formato:** H.264 / MP4, sem áudio. Em dispositivos compatíveis, o Android usa um codificador local (`MediaCodec`), e `MediaMuxer` cria o arquivo. Para minimizar permissões, o app cria um arquivo temporário privado antes de copiá-lo para o destino escolhido pelo usuário. Arquivos temporários são apagados no fim. A duração é proporcional ao número de cenas (pode demorar bastante para capítulos inteiros). A qualidade Rápido é recomendada para o primeiro teste.

## Limitações e segurança

- O resultado é **motion comic**, não anima os corpos ou as expressões dos personagens. Não inclui dublagem, música, OCR nem IA narrativa nesta versão.
- A segmentação é heurística, baseada em espaços em branco; alguns cortes podem interromper balões de diálogo. Por segurança, áreas muito longas têm pequena sobreposição. Correção manual ficará para uma versão futura.
- O MP4 é somente vertical/retrato. Durante a exportação o app precisa permanecer aberto, em primeiro plano e com espaço livre para arquivo temporário + arquivo final.
- **Codec:** exige um encoder H.264 no dispositivo que suporte entrada YUV420 **planar** ou **semiplanar** no modo ByteBuffer. Se não houver, a exportação mostra erro; a leitura continua disponível. Compatibilidade deve ser confirmada em telefone físico.
- O PDF nunca é enviado para a internet; não há declaração de permissão de rede ou permissão geral de armazenamento.
- PDFs protegidos por senha e documentos muito complexos podem falhar.
- Exporte ou compartilhe material protegido por direitos autorais apenas quando tiver autorização ou base legal para fazê-lo.

## Arquitetura

- `MainActivity`: seletor de PDFs e destino MP4, estado do leitor, cancelamento, UI e processamento sequencial do PDF.
- `SceneDetector`: cenas sugeridas por espaço em branco na página (Java puro).
- `PdfSceneRenderer`: renderização de um retângulo PDF em bitmap para os dois modos.
- `SceneTiming`: duração comum às duas modalidades, incluindo tempo de dissolução (Java puro).
- `ScenePainter`: composição de cena, letterbox suave, pan/zoom e crossfade compartilhados com o vídeo.
- `SceneView`: visualização interativa.
- `YuvConverter`: transformação RGB → I420 ou NV12 (Java puro).
- `Mp4Exporter`: renderiza quadros, passa ao encoder H.264 do Android e finaliza MP4 com `MediaMuxer`.

## Compilação do APK

**Android Studio:** abrir a pasta raiz, sincronizar Gradle e executar `app > assembleDebug`. Saída: `app/build/outputs/apk/debug/app-debug.apk`.

**GitHub Actions:** subir o código em um novo repositório; a ação `Build Android APK` executa os testes Java e compila o APK em `assembleDebug`. Artefato `MangaCinema-v0.2.0-debug`. Evite incluir o PDF de teste em repositórios públicos.

- Android Gradle Plugin `8.7.3`; Gradle `8.10.2`; Java `17`; `compileSdk=35`; `minSdk=24`; `targetSdk=35`.
- O ZIP entregue é **código-fonte**, não APK. O ambiente de desenvolvimento usado para gerar este projeto não contém Android SDK, Gradle nem conectividade com os repositórios Android; portanto não foi possível compilar ou executar um APK nele.

## Verificações realizadas

- `SceneDetectorTest`: cenário sintético; 9 cortes candidatos na página 2 do PDF *Nano Machine — Capítulo 38* usando a imagem de prévia produzida no teste anterior. Uma varredura equivalente das 13 páginas estimou 95 cenas no total (~6,9 minutos a 1×).
- `ExportTimingTest`: valida duração, adição da transição, saídas RGB→I420/NV12 e recusa de dimensões ímpares.
- Sintaxe das classes Android verificada com stubs locais, sem garantia de API ou dispositivo físico. Prévia H.264/MP4 gerada por FFmpeg externo apenas como demonstração de resultado.
- **Pendência crítica:** compilação Android real e execução completa do fluxo de exportação em aparelho físico, incluindo cancelamento e player MP4.

## Próximas melhorias

Editor manual dos cortes, duração de cena individual, prévia de vídeo antes de exportar, legenda/voz opcional, correção de enquadramento por balões, criação de capas e perfis alternativos de vídeo.
