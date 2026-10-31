# GoMusic

Mod de cliente para Minecraft que troca a música do jogo por faixas suas, escolhidas conforme a situação do jogador: hora do dia, clima, bioma, dimensão, altura, combate, chefes e outras.

Feito para um modpack pessoal e **testado apenas no Minecraft 1.20.1 com Forge 47.4**. O comportamento e as condições foram inspirados no [Reactive Music](https://github.com/CircuitLord/ReactiveMusic), de CircuitLord. O código foi escrito do zero e não faz parte daquele projeto.

**Nenhuma música vem com o mod.** Você fornece os seus arquivos e é responsável por ter o direito de usá-los. Não redistribua faixas protegidas por direitos autorais junto de um modpack.

## Instalação

1. Coloque o `.jar` na pasta `mods`.
2. Abra o jogo uma vez. O mod cria a pasta `gomusic` e um exemplo, `rules.example.json`.
3. Coloque as faixas em `gomusic/assets/gomusic/sounds/`.
4. Crie `gomusic/rules.json`. Sem esse arquivo, o mod fica inativo e a música normal do jogo continua.

Não é preciso instalar no servidor.

### Formato das faixas

- **Ogg Vorbis (`.ogg`).** `.mp3`, `.mpeg` e `.ogg` com codec Opus não funcionam.
- Nomes só com letras minúsculas, números e `_`, sem espaço nem acento.
- Estéreo, 44.100 ou 48.000 Hz.

Para converter com o ffmpeg:

```
ffmpeg -i "entrada.mp3" -vn -map_metadata -1 -c:a libvorbis -q:a 6 -ar 44100 -ac 2 "saida.ogg"
```

## Regras

A **primeira regra válida** da lista é a que toca. Coloque as mais importantes no topo.

```json
{
  "fadeSeconds": 3,
  "silenceSeconds": 20,
  "switchDelaySeconds": 2,
  "rules": [
    { "when": ["BOSS"], "tracks": ["chefe"], "holdSeconds": 10 },
    { "when": ["MOBS=5"], "tracks": ["combate_1", "combate_2"], "holdSeconds": 15 },
    { "when": ["DIM=nether"], "tracks": ["nether"] },
    { "when": ["NIGHT", "RAIN||STORM"], "tracks": ["noite_chuva"] },
    { "when": ["MAIN_MENU"], "tracks": ["menu"] },
    { "when": ["DAY", "!UNDERGROUND"], "tracks": ["dia_1", "dia_2"], "interrupt": false }
  ]
}
```

| Campo | Significado |
|---|---|
| `when` | Condições. Todas precisam valer; `[]` vale sempre. |
| `tracks` | Faixas, sem a extensão `.ogg`. Se houver mais de uma, o mod sorteia. |
| `holdSeconds` | Mantém a regra por esse tempo depois que a condição acaba. Útil em combate. |
| `interrupt` | Com `false`, espera a faixa atual terminar antes de entrar. O padrão é `true`. |

Dentro de `when`, `A||B` significa "A ou B", e `!A` significa "não A".

### Condições

| Condição | Quando vale |
|---|---|
| `MAIN_MENU`, `CREDITS` | Menu principal; créditos finais |
| `HOME` | A até 45 blocos da última cama em que o jogador dormiu |
| `DAY`, `NIGHT`, `SUNRISE`, `SUNSET` | Hora do dia |
| `RAIN`, `SNOW`, `STORM` | Clima |
| `UNDERWATER` | Debaixo d'água |
| `UNDERGROUND`, `DEEP_UNDERGROUND` | Sem ver o céu, abaixo da altura 55 ou 15 |
| `HIGH_UP` | Vendo o céu, acima da altura 128 |
| `NEARBY_MOBS` | Pelo menos 1 inimigo por perto |
| `MOBS=n` | `n` ou mais inimigos por perto (16 blocos na horizontal, 8 na vertical) |
| `VILLAGE` | Aldeão por perto |
| `BOSS` | Barra de chefe na tela |
| `MINECART`, `BOAT`, `HORSE`, `PIG` | Montado ou dentro do veículo |
| `FISHING` | Pescando |
| `DYING` | Menos de 35% da vida |
| `DAMAGED` | Levou dano nos últimos 5 s, por qualquer causa |
| `BIOME=nome` | Nome do bioma contém o texto. Ex.: `BIOME=forest` |
| `BIOMETAG=tag` | Bioma tem a categoria. Ex.: `BIOMETAG=is_hot` |
| `DIM=nome` | Nome da dimensão contém o texto. Ex.: `DIM=nether` |

A contagem de inimigos é uma aproximação: o cliente não sabe qual alvo cada mob persegue, então conta os que estão por perto, mesmo atrás de uma parede.

## Comandos

| Comando | O que faz |
|---|---|
| `/gomusic status` | Mostra a faixa, as regras e as condições ativas |
| `/gomusic reload` | Recarrega o `rules.json` |
| `/gomusic skip` | Pula a faixa atual |
| `/gomusic reset` | Reinicia o sistema de som, se a música travar |

A música do jogo fica muda enquanto houver regras. Discos de vitrola continuam tocando e abaixam a música do mod.

## Problemas conhecidos

- **Faixa presa:** em um teste, uma faixa ficou minutos sem trocar nem terminar. A causa é desconhecida e o problema não se repetiu. Se acontecer, use `/gomusic reset`.
- **Início repetido:** durante a abertura do jogo ou a carga do mundo, uma faixa pode precisar de mais de uma tentativa para iniciar. A causa não foi confirmada.
- **Condição oscilando:** uma faixa já saindo não volta atrás e recomeça do início. Use `holdSeconds` nas regras de combate.

## Compilar

Requer JDK 17.

```
./gradlew reobfJar
```

O resultado fica em `build/libs/`.

## Licença

[GPL-3.0](LICENSE).
