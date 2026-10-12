# Histórico

## 0.2.0 — dois modos

- Mantém importação PDF, detector de quadros por faixas claras, reprodução cinematográfica e navegação manual.
- Implementa MP4 silencioso por `MediaCodec` + `MediaMuxer` no dispositivo, com transições compartilhadas com o leitor.
- Suporta dois perfis verticais (480×848 a 12 fps, 720×1280 a 15 fps), H.264, progresso, cancelamento e destino escolhido via Android Documents UI.
- Usa a mesma duração de movimento nos dois modos por `SceneTiming`.
- Compartilha o algoritmo de enquadramento em `ScenePainter` e o recorte PDF em `PdfSceneRenderer`.
- Extrai `YuvConverter` (I420/NV12) para testes independentes de Android.
- Adiciona testes de duração/exportação ao fluxo do GitHub Actions.
- Mantém o PDF fora do aplicativo e usa apenas armazenamento privado temporário para o MP4.

## Validações locais

- Compilação e execução de testes Java sem Android: aprovadas.
- Checagem de sintaxe do conjunto de fontes Android contra stubs locais: aprovada (não equivale a compilação SDK/Gradle).
- Scanner do PDF de teste com 13 páginas: 95 cenas candidatas; duração estimada a 1x ~6,94 min.
- Prévia MP4 a partir de quatro cenas com FFmpeg de teste: codec H.264, 480×848, 12fps, duração 17,67s.

## Pendências de validação Android

- `gradle assembleDebug` / SDK Android 35 indisponíveis neste ambiente.
- Confirmar configuração do codec H.264 real no aparelho e reprodução do vídeo exportado.
- Confirmar persistência do acesso ao PDF, salvamento SAF, cancelamento durante renderização e memória em capítulos longos.
