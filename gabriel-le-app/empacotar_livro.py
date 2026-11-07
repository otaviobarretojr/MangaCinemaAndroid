#!/usr/bin/env python3
"""Adiciona as ilustrações de uma história ao arquivo .gbrbook, preservando seus textos.

Uso: python empacotar_livro.py --historia books/gabriel-e-o-portal-misterioso.gbrbook \
    --imagens art/portal-misterioso --saida dist/portal-ilustrado.gbrbook
"""
import argparse
import base64
import json
import mimetypes
from pathlib import Path

EXTS = ('.webp', '.jpg', '.jpeg', '.png')
TYPES = {'.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.webp': 'image/webp'}


def montar(historia: Path, imagens: Path, saida: Path, permitir_faltas=False):
    livro = json.loads(historia.read_text(encoding='utf-8'))
    paginas = livro.get('pages')
    if not isinstance(paginas, list) or not paginas:
        raise ValueError('O arquivo da história precisa ter uma lista de páginas.')
    if len(paginas) > 250:
        raise ValueError('Limite de 250 páginas.')
    presentes, ausentes = [], []
    for numero, pagina in enumerate(paginas, start=1):
        alternativas = [imagens / f'{numero:02d}{ext}' for ext in EXTS]
        arquivos = [p for p in alternativas if p.is_file()]
        if len(arquivos) > 1:
            raise ValueError(f'Página {numero}: mais de uma imagem encontrada.')
        if not arquivos:
            ausentes.append(numero)
            continue
        arquivo = arquivos[0]
        if arquivo.stat().st_size > 25 * 1024 * 1024:
            raise ValueError(f'Imagem da página {numero} acima de 25 MB.')
        mimetype = TYPES[arquivo.suffix.lower()]
        conteudo = base64.b64encode(arquivo.read_bytes()).decode('ascii')
        pagina['image'] = f'data:{mimetype};base64,{conteudo}'
        if numero == 1:
            livro['cover'] = pagina['image']
        presentes.append(numero)
    if ausentes and not permitir_faltas:
        raise ValueError('Faltam imagens das páginas: ' + ', '.join(map(str, ausentes)) + '. Use --permitir-faltas para prévia.')
    saida.parent.mkdir(parents=True, exist_ok=True)
    dados = json.dumps(livro, ensure_ascii=False, separators=(',', ':')).encode('utf-8')
    if len(dados) > 65 * 1024 * 1024:
        raise ValueError('Livro maior que 65 MB: reduza o tamanho das imagens.')
    saida.write_bytes(dados)
    print(f'Livro gerado: {saida} | {len(presentes)} imagens de {len(paginas)} | {len(dados)/1048576:.2f} MB')
    if ausentes:
        print('Páginas sem ilustração:', ', '.join(map(str, ausentes)))


def main():
    a = argparse.ArgumentParser(description=__doc__)
    a.add_argument('--historia', type=Path, required=True)
    a.add_argument('--imagens', type=Path, required=True)
    a.add_argument('--saida', type=Path, required=True)
    a.add_argument('--permitir-faltas', action='store_true')
    args = a.parse_args()
    try:
        montar(args.historia, args.imagens, args.saida, args.permitir_faltas)
    except (ValueError, OSError, json.JSONDecodeError) as ex:
        a.error(str(ex))


if __name__ == '__main__':
    main()