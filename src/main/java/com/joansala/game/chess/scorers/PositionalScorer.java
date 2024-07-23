package com.joansala.game.chess.scorers;

/*
 * Samurai framework.
 * Copyright (C) 2024 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,  either version 3 of the License,  or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not,  see <http://www.gnu.org/licenses/>.
 */

import com.joansala.engine.Scorer;
import com.joansala.game.chess.ChessGame;
import static com.joansala.game.chess.Chess.*;
import static com.joansala.util.bits.Bits.*;


/**
 * This heuristic function evaluates the position of pieces on a
 * chessboard to estimate who has the advantage.
 *
 * It goes beyond just counting captured pieces (material advantage) and
 * considers where each piece is located on the board. Each square on the
 * chessboard has a different value depending on how good it is for a
 * particular piece to occupy that specific square.
 *
 * While it doesn't directly translate to features like "mobility",
 * "attack potential" or "defensive potential" it captures the essence
 * by considering each piece location and its inherent value.
 *
 * The same method used for the Draughts engine was used to fine-tune
 * the evaluation weights. This simple method is described on the
 * {@code PositionalScore} class of the {@code Draughts} module.
 */
public final class PositionalScorer implements Scorer<ChessGame> {

    /**
     * {@inheritDoc}
     */
    public final int evaluate(ChessGame game) {
        int score = 0;

        final long white = game.state(WHITE);
        final long black = game.state(BLACK);

        for (int piece = KING; piece <= PAWN; piece++) {
            long south = white & game.state(piece);
            long north = black & game.state(piece);

            while (empty(south) == false) {
                int checker = first(south);
                score += WEIGHTS[piece][checker];
                south ^= bit(checker);
            }

            while (empty(north) == false) {
                int checker = first(north);
                score -= WEIGHTS[piece][checker ^ 56];
                north ^= bit(checker);
            }
        }

        return score;
    }


    /** Piece-square weights */
    private static final int[][] WEIGHTS = {{
          3,     3,     3,     3,     4,     3,     4,     5,
          3,     2,     3,     2,     2,     3,     3,     3,
         -1,     0,     2,     1,     1,     1,     1,     0,
          0,     1,     0,     0,     0,     1,     0,    -1,
         -1,     0,     0,     1,    -1,    -1,     0,    -1,
         -2,    -1,    -1,    -1,    -1,    -2,    -2,    -2,
         -3,    -2,    -2,    -1,    -2,    -2,    -2,    -3,
         -4,    -3,    -3,    -3,    -3,    -3,    -3,    -4
    }, {
        127,   127,   125,   128,   127,   127,   127,   129,
        123,   123,   123,   121,   124,   120,   120,   124,
        122,   119,   120,   117,   118,   119,   115,   122,
        116,   110,   113,   118,   115,   111,   112,   117,
        121,   115,   115,   113,   114,   113,   116,   120,
        121,   121,   115,   117,   114,   110,   118,   117,
        122,   117,   118,   111,   110,   117,   117,   119,
        127,   122,   122,   120,   118,   117,   121,   129
    }, {
         83,    84,    84,    86,    86,    85,    84,    83,
         81,    80,    81,    80,    81,    79,    78,    81,
         80,    78,    78,    77,    76,    78,    75,    80,
         79,    76,    77,    75,    77,    79,    76,    81,
         83,    81,    78,    79,    79,    79,    78,    83,
         83,    82,    79,    78,    78,    77,    81,    84,
         83,    81,    78,    79,    76,    79,    79,    82,
         86,    82,    81,    83,    83,    81,    82,    86
    }, {
         53,    54,    50,    53,    53,    51,    52,    52,
         53,    53,    53,    52,    53,    53,    52,    53,
         52,    52,    53,    52,    53,    54,    52,    50,
         52,    50,    54,    53,    54,    53,    51,    52,
         51,    51,    54,    54,    55,    52,    51,    51,
         51,    52,    51,    52,    52,    53,    51,    50,
         51,    52,    51,    49,    49,    49,    52,    49,
         51,    49,    49,    51,    49,    49,    49,    50
    }, {
         41,    40,    42,    42,    42,    42,    39,    39,
         41,    41,    44,    43,    43,    43,    41,    40,
         40,    42,    43,    46,    45,    44,    42,    40,
         42,    43,    46,    45,    45,    45,    44,    40,
         43,    45,    46,    47,    46,    46,    45,    41,
         41,    43,    44,    46,    46,    44,    42,    41,
         41,    39,    43,    43,    42,    43,    40,    40,
         40,    40,    40,    41,    40,    42,    39,    36
    }, {
          0,     0,     0,     0,     0,     0,     0,     0,
         26,    23,    24,    22,    22,    24,    23,    26,
         23,    22,    22,    22,    22,    22,    23,    24,
         22,    23,    22,    21,    22,    22,    23,    22,
         27,    27,    25,    25,    24,    25,    26,    26,
         43,    38,    37,    35,    34,    35,    38,    41,
         76,    70,    64,    63,    61,    64,    67,    74,
          0,     0,     0,     0,     0,     0,     0,     0
    }};
}
