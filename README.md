# GoMusic

[English](#english) | [Português](#português)

## English

A client-side Minecraft mod that replaces the game's music with your own tracks, chosen by what the player is doing: time of day, weather, biome, dimension, height, combat, bosses and more.

Built for a personal modpack and **tested only on Minecraft 1.20.1 with Forge 47.4**. Its behavior and conditions were inspired by [Reactive Music](https://github.com/CircuitLord/ReactiveMusic) by CircuitLord. The code was written from scratch and is not part of that project.

**No music is included.** You provide your own files and are responsible for having the right to use them. Do not redistribute copyrighted tracks with a modpack.

## Installation

1. Put the `.jar` in the `mods` folder.
2. Start the game once. The mod creates a `gomusic` folder with a `rules.example.json` file.
3. Put your tracks in `gomusic/assets/gomusic/sounds/`.
4. Create `gomusic/rules.json`. Without this file, the mod stays inactive and the normal game music keeps playing.

The mod does not need to be installed on the server.

### Track format

- **Ogg Vorbis (`.ogg`).** `.mp3`, `.mpeg` and `.ogg` files using the Opus codec do not work.
- File names may contain only lowercase letters, numbers and `_`, with no spaces or accents.
- Stereo, at 44,100 or 48,000 Hz.

To convert a file with ffmpeg:

```
ffmpeg -i "input.mp3" -vn -map_metadata -1 -c:a libvorbis -q:a 6 -ar 44100 -ac 2 "output.ogg"
```

## Rules

The **first valid rule** in the list is the one that plays, so put the most important rules at the top.

```json
{
  "fadeSeconds": 3,
  "silenceSeconds": 20,
  "switchDelaySeconds": 2,
  "rules": [
    { "when": ["BOSS"], "tracks": ["boss"], "holdSeconds": 10 },
    { "when": ["MOBS=5"], "tracks": ["combat_1", "combat_2"], "holdSeconds": 15 },
    { "when": ["DIM=nether"], "tracks": ["nether"] },
    { "when": ["NIGHT", "RAIN||STORM"], "tracks": ["rainy_night"] },
    { "when": ["MAIN_MENU"], "tracks": ["menu"] },
    { "when": ["DAY", "!UNDERGROUND"], "tracks": ["day_1", "day_2"], "interrupt": false }
  ]
}
```

| Field | Meaning |
|---|---|
| `when` | Conditions. All of them must be true; `[]` is always true. |
| `tracks` | Track names, without the `.ogg` extension. If there is more than one, the mod picks at random. |
| `holdSeconds` | Keeps the rule active for this long after its conditions end. Useful for combat. |
| `interrupt` | When `false`, waits for the current track to finish before starting. Defaults to `true`. |

Inside `when`, `A||B` means "A or B", and `!A` means "not A".

### Conditions

| Condition | True when |
|---|---|
| `MAIN_MENU`, `CREDITS` | On the main menu; during the end credits |
| `HOME` | Within 45 blocks of the last bed the player slept in |
| `DAY`, `NIGHT`, `SUNRISE`, `SUNSET` | Time of day |
| `RAIN`, `SNOW`, `STORM` | Weather |
| `UNDERWATER` | Underwater |
| `UNDERGROUND`, `DEEP_UNDERGROUND` | In the Overworld without sky access, below height 55 or 15 |
| `HIGH_UP` | In the Overworld with sky access, above height 128 |
| `NEARBY_MOBS` | At least 1 enemy nearby |
| `MOBS=n` | `n` or more enemies nearby (16 blocks horizontally, 8 vertically) |
| `VILLAGE` | A villager is nearby |
| `BOSS` | A boss bar is on screen |
| `MINECART`, `BOAT`, `HORSE`, `PIG` | Riding that vehicle or animal |
| `FISHING` | Fishing |
| `DYING` | Below 35% health |
| `DAMAGED` | Took damage from any source in the last 5 seconds |
| `BIOME=name` | The biome name contains the text, e.g. `BIOME=forest` |
| `BIOMETAG=tag` | The biome has a matching tag, e.g. `BIOMETAG=is_hot` |
| `DIM=name` | The dimension name contains the text, e.g. `DIM=nether` |

The enemy count is an approximation: the client does not know which target each mob is chasing, so it counts every enemy nearby, even behind a wall.

## Commands

| Command | What it does |
|---|---|
| `/gomusic status` | Shows the current track, rules and active conditions |
| `/gomusic reload` | Reloads `rules.json` |
| `/gomusic skip` | Skips the current track |
| `/gomusic reset` | Restarts the sound system if the music gets stuck |

The game's own music stays muted while there are rules. Jukebox records keep playing and lower the mod's music.

## Known issues

- **Stuck track:** in one test, a track stayed on for minutes without switching or ending. The cause is unknown and the problem did not happen again. If it does, use `/gomusic reset`.
- **Repeated start:** while the game opens or a world loads, a track may need more than one attempt to start. The cause has not been confirmed.
- **Flickering condition:** a track that is already fading out does not recover and starts over from the beginning. Use `holdSeconds` on combat rules.

## Building

Requires JDK 17.

```
./gradlew reobfJar
```

The output goes to `build/libs/`.

## License

[GPL-3.0](LICENSE).

---

## Português

Mod de cliente para Minecraft que troca a música do jogo por faixas suas, escolhidas conforme a situação do jogador: hora do dia, clima, bioma, dimensão, altura, combate, chefes e outras.

Feito para um modpack pessoal e **testado apenas no Minecraft 1.20.1 com Forge 47.4**. O comportamento e as condições foram inspirados no [Reactive Music](https://github.com/CircuitLord/ReactiveMusic), de CircuitLord. O código foi escrito do zero e não faz parte daquele projeto.

**Nenhuma música vem com o mod.** Você fornece os seus arquivos e é responsável por ter o direito de usá-los. Não redistribua faixas protegidas por direitos autorais junto de um modpack.

### Instalação

1. Coloque o `.jar` na pasta `mods`.
2. Abra o jogo uma vez. O mod cria a pasta `gomusic` com o arquivo `rules.example.json`.
3. Coloque as faixas em `gomusic/assets/gomusic/sounds/`.
4. Crie `gomusic/rules.json`. Sem esse arquivo, o mod fica inativo e a música normal do jogo continua.

Não é preciso instalar no servidor.

#### Formato das faixas

- **Ogg Vorbis (`.ogg`).** `.mp3`, `.mpeg` e `.ogg` com codec Opus não funcionam.
- Nomes só com letras minúsculas, números e `_`, sem espaço nem acento.
- Estéreo, 44.100 ou 48.000 Hz.

Para converter com o ffmpeg:

```
ffmpeg -i "entrada.mp3" -vn -map_metadata -1 -c:a libvorbis -q:a 6 -ar 44100 -ac 2 "saida.ogg"
```

### Regras

A **primeira regra válida** da lista é a que toca, então coloque as mais importantes no topo. O formato é o do exemplo em JSON da seção em inglês.

| Campo | Significado |
|---|---|
| `when` | Condições. Todas precisam valer; `[]` vale sempre. |
| `tracks` | Faixas, sem a extensão `.ogg`. Se houver mais de uma, o mod sorteia. |
| `holdSeconds` | Mantém a regra por esse tempo depois que as condições acabam. Útil em combate. |
| `interrupt` | Com `false`, espera a faixa atual terminar antes de entrar. O padrão é `true`. |

Dentro de `when`, `A||B` significa "A ou B", e `!A` significa "não A".

#### Condições

| Condição | Quando vale |
|---|---|
| `MAIN_MENU`, `CREDITS` | No menu principal; nos créditos finais |
| `HOME` | A até 45 blocos da última cama em que o jogador dormiu |
| `DAY`, `NIGHT`, `SUNRISE`, `SUNSET` | Hora do dia |
| `RAIN`, `SNOW`, `STORM` | Clima |
| `UNDERWATER` | Debaixo d'água |
| `UNDERGROUND`, `DEEP_UNDERGROUND` | No Overworld, sem ver o céu, abaixo da altura 55 ou 15 |
| `HIGH_UP` | No Overworld, vendo o céu, acima da altura 128 |
| `NEARBY_MOBS` | Pelo menos 1 inimigo por perto |
| `MOBS=n` | `n` ou mais inimigos por perto (16 blocos na horizontal, 8 na vertical) |
| `VILLAGE` | Aldeão por perto |
| `BOSS` | Barra de chefe na tela |
| `MINECART`, `BOAT`, `HORSE`, `PIG` | Montado ou dentro do veículo |
| `FISHING` | Pescando |
| `DYING` | Menos de 35% da vida |
| `DAMAGED` | Levou dano de qualquer fonte nos últimos 5 s |
| `BIOME=nome` | O nome do bioma contém o texto. Ex.: `BIOME=forest` |
| `BIOMETAG=tag` | O bioma tem a categoria. Ex.: `BIOMETAG=is_hot` |
| `DIM=nome` | O nome da dimensão contém o texto. Ex.: `DIM=nether` |

A contagem de inimigos é uma aproximação: o cliente não sabe qual alvo cada mob persegue, então conta todos os que estão por perto, mesmo atrás de uma parede.

### Comandos

| Comando | O que faz |
|---|---|
| `/gomusic status` | Mostra a faixa, as regras e as condições ativas |
| `/gomusic reload` | Recarrega o `rules.json` |
| `/gomusic skip` | Pula a faixa atual |
| `/gomusic reset` | Reinicia o sistema de som, se a música travar |

A música do jogo fica muda enquanto houver regras. Discos de vitrola continuam tocando e abaixam a música do mod.

### Problemas conhecidos

- **Faixa presa:** em um teste, uma faixa ficou minutos sem trocar nem terminar. A causa é desconhecida e o problema não se repetiu. Se acontecer, use `/gomusic reset`.
- **Início repetido:** durante a abertura do jogo ou a carga do mundo, uma faixa pode precisar de mais de uma tentativa para iniciar. A causa não foi confirmada.
- **Condição oscilando:** uma faixa já saindo não volta atrás e recomeça do início. Use `holdSeconds` nas regras de combate.

### Compilar

Requer JDK 17.

```
./gradlew reobfJar
```

O resultado fica em `build/libs/`.

### Licença

[GPL-3.0](LICENSE).
