package com.joansala.engine;

/*
 * Copyright (C) 2014 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Scanner;


/**
 * Provides a client communication interface to an UCI service. This is
 * currently only a partial implementation of the UCI protocol; not all
 * the possible UCI commands are completely parsed.</p>
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class UCIClient {
    
    /** Command regular expression */
    private static final Pattern pattern =
        Pattern.compile("^[ \\t]*(\\S+)(?:[ \\t]+(.*))?$");
    
    /** Engine process */
    private Process service;
    
    /** Game object */
    private Game game = null;
    
    /** Contains the current start position and turn */
    private Board board = null;
    
    /** Contains the initial board of the game */
    private Board start = null;
    
    /** Current state of the engine */
    private State state = State.STOPPED;
    
    /** Current engine name */
    private String name = "Unknown";
    
    /** Current engine author */
    private String author = "Unknown";
    
    /** Engine process input scanner */
    private Scanner input = null;
    
    /** Engine process output stream */
    private PrintStream output = null;
    
    /** Latest best move received */
    private int bestMove = Game.NULL_MOVE;
    
    /** Latest ponder move received */
    private int ponderMove = Game.NULL_MOVE;
    
    /** Infinite mode status flag */
    private boolean infinite = false;
    
    /** Debug mode status flag */
    private boolean debug = false;
    
    /** Used for synchronization */
    private boolean ready = false;
    
    /** UCI protocol acknowledgement */
    private boolean uciok = true;
    
    
    /** Possible engine states */
    public enum State {
        
        /** The engine process is not running */
        STOPPED,
        
        /** The engine is waiting for commands */
        WAITING,
        
        /** The engine is calculating a move */
        THINKING,
        
        /** The engine is pondering a move */
        PONDERING
    }
    
    
    /**
     * Instantiates a new client object. This constructor takes as
     * parameters an engine process, a starting board for the game and
     * a game object where moves will be performed.
     *
     * @param service   An engine process
     * @param start     Start board position
     * @param game      A game object
     */
    public UCIClient(Process service, Board start, Game game) {
        InputStream input = service.getInputStream();
        OutputStream output = service.getOutputStream();
        
        this.input = new Scanner(input);
        this.output = new PrintStream(output, true);
        
        this.service = service;
        this.start = start;
        this.board = start;
        this.game = game;
        this.state = State.WAITING;
        this.ready = true;
        this.uciok = true;
    }
    
    
    /**
     * Returns the engine process associated with this client.
     *
     * @return  Engine process
     */
    public Process getService() {
        return this.service;
    }
    
    
    /**
     * Returns a board representation of the current game state.
     *
     * @return  A board object
     */
    public Board getBoard() {
        return start.toBoard(game);
    }
    
    
    /**
     * Returns the current engine state. Note that this method returns
     * the last known state of the engine; it may differ from the real
     * engine process state.
     *
     * @return  Engine state
     */
    public State getState() {
        return state;
    }
    
    
    /**
     * Returns the last received best move.
     *
     * @return  A move identifier or {@code Game.NULL_MOVE}
     *          if no move has been received yet
     */
    public int getBestMove() {
        return bestMove;
    }
    
    
    /**
     * Returns the last received ponder move.
     *
     * @return  A move identifier or {@code Game.NULL_MOVE}
     *          if no move has been received yet
     */
    public int getPonderMove() {
        return ponderMove;
    }
    
    
    /**
     * Returns the current engine name.
     *
     * @return  Engine name
     */
    public String getName() {
        return name;
    }
    
    
    /**
     * Returns the current engine author name.
     *
     * @return  engine author name
     */
    public String getAuthor() {
        return author;
    }
    
    
    /**
     * Returns the current engine debug mode.
     *
     * @return  {@code true} if debug is on
     */
    public boolean isDebugOn() {
        return debug;
    }
    
    
    /**
     * Returns if the current engine is not thinking in infinite mode.
     * Infinite mode is enabled after a 'go ponder' or 'go infinite'
     * is received and set to {@code false} when a 'stop' command is
     * sent to the engine or a 'bestmove' command is received. If the
     * engine is not thinking this method always returns {@code true}.
     *
     * @return  {@code false} if infinite mode is on
     */
    public boolean hasTimeLimit() {
        return !infinite;
    }
    
    
    /**
     * Returns true if the engine is in a thinking state. That is, the
     * last known state is {@code THINKING}.
     *
     * @return  {@code true} if the engine is thinking
     */
    public boolean isThinking() {
        return (state == State.THINKING);
    }
    
    
    /**
     * Returns true if the engine is in a pondering state. That is, the
     * last known state is {@code PONDERING}.
     *
     * @return  {@code true} if the engine is thinking
     */
    public boolean isPondering() {
        return (state == State.PONDERING);
    }
    
    
    /**
     * Returns if the engine process is running. That is, if the last
     * known state of the engine is not {@code STOPPED}.
     *
     * @return  {@code true} if the engine process is running
     */
    public boolean isRunning() {
        return (state != State.STOPPED);
    }
    
    
    /**
     * Returns true whenever the engine is ready to receive new commands.
     * The ready state is set to {@code false} when an 'isready' command
     * is sent to the engine, and is reset to {@code true} after a
     * 'readyok' message is received. When the engine state is
     * {@code STOPPED} this method always returns {@code false}.
     *
     * @return  {@code true} if the engine is ready
     */
    public boolean isReady() {
        if (state == State.STOPPED)
            return false;
        
        return ready;
    }
    
    
    /**
     * This method returns true whenever the engine is ready to receive
     * commands in UCI mode. The ready state is set to {@code false} when
     * an 'uci' command is sent to the engine, and is reset to {@code true}
     * after a 'uciok' message is received. When the engine state is
     * {@code STOPPED} this method always returns {@code false}.
     *
     * @return  {@code true} if the engine supports the UCI protocol
     *          and is ready to receive commands
     */
    public boolean isUCIReady() {
        if (state == State.STOPPED)
            return false;
        
        return uciok;
    }
    
    
    /**
     * Parses the parameters of a 'uci' command and sets the relevant
     * engine state information.
     *
     * @param params    command parameters
     */
    private void parseUCI(String params) throws Exception {
        this.uciok = false;
    }
    
    
    /**
     * Parses the parameters of a 'debug' command and sets the relevant
     * engine state information.
     *
     * @param params    command parameters
     */
    private void parseDebug(String params) throws Exception {
        boolean debugSwitch = true;
        boolean debugValue = true;
        
        if (params != null) {
            Scanner scanner = new Scanner(params);
            
            while (scanner.hasNext()) {
                String token = scanner.next();
                
                if (token.equals("on")) {
                    debugSwitch = false;
                    debugValue = true;
                } else if (token.equals("off")) {
                    debugSwitch = false;
                    debugValue = false;
                }
            }
            
            scanner.close();
        }
        
        debug = (debugSwitch == true) ?
            !debug : debugValue;
    }
    
    
    /**
     * Parses the parameters of an 'isready' command and sets the
     * relevant engine state information.
     *
     * @param params    command parameters
     */
    private void parseIsReady(String params) throws Exception {
        this.ready = false;
    }
    
    
    /**
     * Parses the parameters of a 'setoption' command and sets the
     * relevant engine state information.
     *
     * @param params    command parameters
     */
    private void parseSetOption(String params) throws Exception {
        // Not implemented
    }
    
    
    /**
     * Parses the parameters of a 'register' command and sets the
     * relevant engine state information.
     *
     * @param params    command parameters
     */
    private void parseRegister(String params) throws Exception {
        // Not implemented
    }
    
    
    /**
     * Parses the parameters of an 'ucinewgame' command and sets
     * the relevant engine state information.
     *
     * @param params    command parameters
     * @throws IllegalStateException    if current engine
     *      state is not {@code WAITING}
     */
    private void parseUCINewGame(String params) throws Exception {
        if (state != State.WAITING) {
            throw new IllegalStateException(
                "The engine is not waiting for commands");
        }
    }
    
    
    /**
     * Parses the parameters of a 'position' command and sets the
     * relevant engine state information. This method validates the
     * provided position and moves and throws an exception if the
     * position cannot be set.
     *
     * @param params    command parameters
     * @throws IllegalArgumentException if one or more of the
     *      parameters cannot be set
     */
    private void parsePosition(String params) throws Exception {
        String position = null;
        String notation = null;
        Board newBoard = null;
        int[] moves = null;
        
        // This command requieres at least one parameter
        
        if (params == null) {
            throw new IllegalArgumentException(
                "No parameters were provided");
        }
        
        // Parse the provided parameters
        
        Scanner scanner = new Scanner(params);
        Pattern stop = Pattern.compile("startpos|fen|moves");
        
        while (scanner.hasNext()) {
            String token = scanner.next();
            
            if ("startpos".equals(token)) {
                position = "startpos";
            } else if ("fen".equals(token)) {
                if (scanner.hasNext())
                    position = scanner.next();
            } else if ("moves".equals(token)) {
                notation = consumeString(scanner, stop);
            }
        }
        
        scanner.close();
        
        // Obtain the board for the received position
        
        if (position == null) {
            throw new IllegalArgumentException(
                "No position was provided");
        }
        
        board = ("startpos".equals(position)) ?
            start : start.toBoard(position);
        
        game.setStart(board.position(), board.turn());
        
        // Obtain the moves for the received notation
        
        if (notation != null)
            moves = start.toMoves(notation);
        
        // Change the game state only if all moves are legal
        
        if (moves == null)
            return;
        
        int madeCount = 0;
        game.ensureCapacity(moves.length);
        
        for (int move : moves) {
            if (!game.isLegal(move)) {
                for (int i = 0; i < madeCount; i++)
                    game.unmakeMove();
                throw new IllegalArgumentException(
                    "The provided moves are not legal");
            }
            
            game.makeMove(move);
            madeCount++;
        }
    }
    
    
    /**
     * Parses the parameters of a 'go' command and sets the relevant
     * engine state information.
     *
     * @param params    command parameters
     * @throws IllegalStateException    if the engine is thinking
     */
    private void parseGo(String params) throws Exception {
        boolean infinite = false;
        boolean ponder = false;
        
        // Ensure the engine is in a consistent state
        
        if (state != State.WAITING) {
            throw new IllegalStateException(
                "The engine is already thinking");
        }
        
        // Parse the provided parameters
        
        if (params != null) {
            Scanner scanner = new Scanner(params);
            
            while (scanner.hasNext()) {
                String token = scanner.next();
                
                if ("infinite".equals(token)) {
                    infinite = true;
                } else if ("ponder".equals(token)) {
                    ponder = true;
                    infinite = true;
                }
            }
            
            scanner.close();
        }
        
        // Change the engine state accordingly
        
        this.state = (ponder == true) ?
            State.PONDERING : State.THINKING;
        this.infinite = infinite;
    }
    
    
    /**
     * Parses the parameters of an 'stop' command and sets the relevant
     * engine state information. This method does not change the state of
     * the engine process.
     *
     * @param params    command parameters
     * @throws IllegalStateException    if the current engine
     *      state is not {@code THINKING} or {@code PONDERING}
     */
    private void parseStop(String params) throws Exception {
        if (state != State.THINKING && state != State.PONDERING) {
            throw new IllegalStateException(
                "The engine is not thinking");
        }
        
        this.infinite = false;
    }
    
    
    /**
     * Parses the parameters of a 'quit' command and sets the relevant
     * engine state information. If the engine state was {@code PONDERING},
     * this method changes its state to {@code THINKING}.
     *
     * @param params    command parameters
     * @throws IllegalStateException    if the current engine
     *      state is not {@code PONDERING}
     */
    private void parsePonderHit(String params) throws Exception {
        if (state != State.PONDERING) {
            throw new IllegalStateException(
                "The engine is not pondering");
        }
        
        state = State.THINKING;
    }
    
    
    /**
     * Parses the parameters of a 'quit' command and sets the
     * relevant engine state information.
     *
     * @param params    command parameters
     */
    private void parseQuit(String params) throws Exception {
        this.state = State.STOPPED;
    }
    
    
    /**
     * Parses the parameters of an 'id' command and sets the
     * relevant engine state information.
     *
     * @param params    command parameters
     */
    private void parseID(String params) throws Exception {
        if (params == null) {
            throw new IllegalArgumentException(
                "No parameters were provided");
        }
        
        Scanner scanner = new Scanner(params);
        Pattern stop = Pattern.compile("name|value");
        
        while (scanner.hasNext()) {
            String token = scanner.next();
            
            if (token.equals("name")) {
                this.name = consumeString(scanner, stop);
            } else if (token.equals("author")) {
                this.author = consumeString(scanner, stop);
            }
        }
        
        scanner.close();
    }
    
    
    /**
     * Parses the parameters of an 'uciok' command and sets the
     * relevant engine state information.
     *
     * @param params    command parameters
     */
    private void parseUCIOk(String params) throws Exception {
        this.uciok = true;
    }
    
    
    /**
     * Parses the parameters of an 'readyok' command and sets the
     * relevant engine state information.
     *
     * @param params    command parameters
     */
    private void parseReadyOk(String params) throws Exception {
        this.ready = true;
    }
    
    
    /**
     * Parses the parameters of a 'bestmove' command and sets the relevant
     * engine state information. This method validates the provided moves
     * against the current position.
     *
     * @param params    command parameters
     * @throws IllegalStateException  if the engine was not thinking
     * @throws IllegalArgumentException if the moves are not valid
     */
    private void parseBestMove(String params) throws Exception {
        String bestMove = null;
        String ponderMove = null;
        
        // Change the state, even if moves are not valid
        
        this.state = State.WAITING;
        this.infinite = false;
        
        // This method requieres at least a parameter
        
        if (params == null) {
            throw new IllegalArgumentException(
                "No parameters were provided");
        }
        
        // Parse the provided parameters
        
        Scanner scanner = new Scanner(params);
        
        if (scanner.hasNext())
            bestMove = scanner.next();
        
        while (scanner.hasNext()) {
            String token = scanner.next();
            
            if ("ponder".equals(token)) {
                if (scanner.hasNext())
                    ponderMove = scanner.next();
            }
        }
        
        scanner.close();
        
        // Check if a null move was received
        
        if ("0000".equals(bestMove)) {
            this.bestMove = Game.NULL_MOVE;
            this.ponderMove = Game.NULL_MOVE;
            return;
        }
        
        // Validate the received moves legality
        
        int best = start.toMove(bestMove);
        int ponder = Game.NULL_MOVE;
        
        if (!game.isLegal(best)) {
            throw new IllegalArgumentException(
                "The returned move is not legal");
        }
        
        game.ensureCapacity(2 + game.length());
        game.makeMove(best);
        
        try {
            if (ponderMove != null) {
                ponder = start.toMove(ponderMove);
                
                if (!game.isLegal(ponder)) {
                    throw new IllegalArgumentException(
                        "The returned move is not legal");
                }
            }
        } catch (Exception e) {
            throw e;
        }
        
        game.unmakeMove();
        
        // Save the received moves
        
        this.bestMove = best;
        this.ponderMove = ponder;
    }
    
    
    /**
     * Parses the parameters of a 'copyprotection' command and sets
     * the relevant engine state information.
     *
     * @param params    command parameters
     */
    private void parseCopyProtection(String params) throws Exception {
        // Not implemented
    }
    
    
    /**
     * Parses the parameters of a 'registration' command and sets
     * the relevant engine state information.
     *
     * @param params    command parameters
     */
    private void parseRegistration(String params) throws Exception {
        // Not implemented
    }
    
    
    /**
     * Parses the parameters of an 'info' command and sets the
     * relevant engine state information.
     *
     * @param params    command parameters
     */
    private void parseInfo(String params) throws Exception {
        // Not implemented
    }
    
    
    /**
     * Parses the parameters of an 'option' command and sets the
     * relevant engine state information.
     *
     * @param params    command parameters
     */
    private void parseOption(String params) throws Exception {
        // Not implemented
    }
    
    
    /**
     * Builds a {@code String} from the {@code Scanner} concatenating
     * each token until it founds a token that matches the stop pattern
     * or the end of input is reached.
     *
     * @param scanner  The scanner from which to consume tokens
     * @param stop     The stop pattern
     */
    private static String consumeString(Scanner scanner, Pattern stop) {
        StringBuilder sb = new StringBuilder();
        
        while (scanner.hasNext() && !scanner.hasNext(stop)) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(scanner.next());
        }
        
        return sb.toString();
    }
    
    
    /**
     * Evaluates an engine-to-client command.
     *
     * @param message       command string
     * @throws Exception    if the evaluation did not succeed
     */
    private void evaluateInput(String message) throws Exception {
        // Parse the input message
        
        Matcher matcher = pattern.matcher(message);
        
        if (matcher.matches() == false) {
            throw new IllegalArgumentException(
                "Syntax error: " + message);
        }
        
        // Parse the received command
        
        String command = matcher.group(1);
        String params = matcher.group(2);
        
        if ("id".equals(command)) {
            parseID(params);
        } else if ("uciok".equals(command)) {
            parseUCIOk(params);
        } else if ("readyok".equals(command)) {
            parseReadyOk(params);
        } else if ("bestmove".equals(command)) {
            parseBestMove(params);
        } else if ("copyprotection".equals(command)) {
            parseCopyProtection(params);
        } else if ("registration".equals(command)) {
            parseRegistration(params);
        } else if ("info".equals(command)) {
            parseInfo(params);
        } else if ("option".equals(command)) {
            parseOption(params);
        } else {
            throw new IllegalArgumentException(
                "Unknown engine command: " + command);
        }
    }
    
    
    /**
     * Evaluates a client-to-engine command.
     *
     * @param message       command string
     * @throws Exception    if the evaluation did not succeed
     */
    private void evaluateOutput(String message) throws Exception {
        // Ensure that the engine is running
        
        if (state == State.STOPPED) {
            throw new IllegalStateException(
                "The engine is not running");
        }
        
        // Parse the output message
        
        Matcher matcher = pattern.matcher(message);
        
        if (matcher.matches() == false) {
            throw new IllegalArgumentException(
                "Syntax error: " + message);
        }
        
        // Parse the received command
        
        String command = matcher.group(1);
        String params = matcher.group(2);
        
        if ("uci".equals(command)) {
            parseUCI(params);
        } else if ("debug".equals(command)) {
            parseDebug(params);
        } else if ("isready".equals(command)) {
            parseIsReady(params);
        } else if ("setoption".equals(command)) {
            parseSetOption(params);
        } else if ("register".equals(command)) {
            parseRegister(params);
        } else if ("ucinewgame".equals(command)) {
            parseUCINewGame(params);
        } else if ("position".equals(command)) {
            parsePosition(params);
        } else if ("go".equals(command)) {
            parseGo(params);
        } else if ("stop".equals(command)) {
            parseStop(params);
        } else if ("ponderhit".equals(command)) {
            parsePonderHit(params);
        } else if ("quit".equals(command)) {
            parseQuit(params);
        } else {
            throw new IllegalArgumentException(
                "Unknown client command: " + command);
        }
    }
    
    
    /**
     * Evaluates and sends a single client-to-engine command. This method
     * evaluates a single command and if this succeeds the messages is sent
     * to the engine process and the current state of the engine is updated.
     *
     * @param message       command string
     * @throws Exception    if the evaluation did not succeed
     */
    public void send(String message) throws Exception {
        evaluateOutput(message);
        output.format("%s%n", message);
    }
    
    
    /**
     * Receives and evaluates the next engine-to-client command. Commands
     * are read from the current engine process input stream. Calling this
     * method may produce changes on the current sate of the engine.
     *
     * @return              received command string
     * @throws Exception    if the evaluation did not succeed
     */
    public String receive() throws Exception {
        String message = null;
        
        if (input.hasNextLine()) {
            message = input.nextLine();
            if (!message.isEmpty())
                evaluateInput(message);
        } else {
            throw new IllegalStateException(
                "Engine process is not responding");
        }
        
        return message;
    }
    
}
