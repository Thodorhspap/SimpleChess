# SimpleChess

A complete chess game in Java Swing. Play against the computer or with a friend on the same PC.

## Features

- **Full chess rules:** legal moves for every piece, check, checkmate, stalemate, castling, en passant, pawn promotion (choose queen, rook, bishop or knight), and a draw when too few pieces are left to checkmate
- **Computer opponent** with three difficulty levels:
  - Easy looks 1 move ahead, with some randomness
  - Medium looks 3 moves ahead
  - Hard looks 4 moves ahead
- **Start menu:** choose the mode, the difficulty and your colour (White, Black or Random). Playing Black flips the board.
- **Wooden board:** the maple and walnut textures are generated in code, so no image files are needed
- **Helpful highlights:** legal moves, captures, the last move and a king in check
- **End-of-game screen:** congratulations when you win, and a button back to the menu

## Controls

| Action | How |
|---|---|
| Move a piece | Drag it, or click it and then click the target square |
| Start a game from the menu | Click **Start game** or press **Enter** |
| Back to the menu | **R** or **Esc**, or **Return to menu** after a game |

## How to run

Requires Java 17 or newer.

**IntelliJ IDEA:** open the folder and run `main.main`.

**Command line:**
```
javac -d out src/main/*.java
cp -r src/piece out/        # Windows: xcopy /E /I src\piece out\piece
java -cp out main.main
```

## How the computer plays

`Computer.java` uses **minimax with alpha-beta pruning**. It scores positions by piece values plus piece-square tables (which reward centre control, king safety and advanced pawns). It also uses a **quiescence search**, which keeps following capture sequences past the depth limit so it doesn't misjudge a position in the middle of a trade. It thinks on a background thread, so the window keeps responding.

## Project structure

| File | What it does |
|---|---|
| `main.java` | Creates the window |
| `GamePanel.java` | Game state, rules (check, castling, en passant, promotion, game end), drawing, menu and end-of-game screen |
| `Board.java` | Draws the wooden board |
| `piece.java` | Base class for pieces, with shared movement helpers |
| `Pawn`, `Rook`, `Knight`, `Bishop`, `Queen`, `King` | Each piece's movement rules |
| `Computer.java` | The computer opponent |
| `Mouse.java` | Mouse input |
| `Type.java` | Piece types |
