# Flávio na Casa Branca — A Aventura

Um *point-and-click* autenticamente ruim de 1994. Nada de remaster, nada de
"retro revival" — é o tipo de jogo que rodava naquele computador que a gente
chamava de cafeteira. 320×200, paleta cafona, "PC Speaker" e humor
constrangedor (vergonha alheia, estilo padrão).

## A história

Você é **Flávio**. Você disse pra todo mundo na padaria que foi convidado pra
uma reunião com o presidente dos EUA. Você **não** foi convidado. Mas já
comprou a passagem e alugou o terno do seu pai. Voltar sem uma selfie com o
presidente seria vergonha demais.

**Objetivo:** entrar na Casa Branca e tirar a foto. Custe a dignidade que custar.

## Como jogar

Feito pra **celular na vertical**. A cena em pixel-art fica no topo; embaixo,
uma área de texto grande e legível, botões de verbo enormes e o inventário.

1. Toque num **verbo** (Olhar, Pegar, Usar, Falar, Dar, Ir p/).
2. Toque numa coisa da cena.
3. Para usar/dar um **item**, toque nele no inventário e depois no alvo.

Funciona com toque (celular) e mouse (desktop).

### Por dentro

A charada de legibilidade no celular foi resolvida separando as camadas:
o `<canvas>` desenha **só** a cena pixel-art (320×150, escalada com
nearest-neighbor), e **todo** o texto e os controles são HTML/CSS crisp em
tamanho grande — nada de fonte borrada saindo de um buffer de baixa resolução.

> Dica anti-frustração noventista: tudo que você precisa está na calçada.
> Lixo, cachorro-quente e um esquilo são uma combinação mais poderosa do que
> parece.

## Rodar

É um único `index.html`, sem dependências. Abra o arquivo no navegador, ou
acesse pela versão hospedada no GitHub Pages.

### GitHub Pages

Com o Pages habilitado para o repositório (Settings → Pages → Deploy from
branch → `main` / root), o jogo fica em:

```
https://<usuario>.github.io/ohie/flavio-casa-branca/
```

(c) 1994 TorteSoft (não). Disco 1 de 1.
