package com.codeoftheweb.salvo;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@RestController
@RequestMapping("/api")
public class SalvoController {



    @Autowired
    private GameRepository gameRepository;

    @RequestMapping(path = "/games", method = RequestMethod.GET)
    public Map<String, Object> getGames(Authentication authentication) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        if (authentication != null) {
            dto.put("player", makePlayerDTO(getCurrentUser(authentication)));
        } else {
            dto.put("player", "no player logged in");
        }
        dto.put("games", gameRepository
                .findAll()
                .stream()
                .map(game -> makeGameDTO(game))
                .collect(toList()));
        return dto;
    }

    @RequestMapping(path = "/games", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createGame(Authentication authentication) {
        if (authentication != null) {
            Date date = new Date();
            Game game = new Game(date, false);
            gameRepository.save(game);
            Player player = playerRepository.findByUserName(authentication.getName());
            GamePlayer gamePlayer = new GamePlayer(player, game, date);
            gameplayerRepository.save(gamePlayer);
            return new ResponseEntity<>(makeMap("gpid", gamePlayer.getId()), HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(makeMap("gpid", "no player logged in"), HttpStatus.UNAUTHORIZED);
        }
    }

    @Autowired
    private PlayerRepository playerRepository;

    @RequestMapping(path = "/players", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createUser(@RequestParam String name, String pwd) {
        if (name.isEmpty()) {
            return new ResponseEntity<>(makeMap("error", "No name provided"), HttpStatus.FORBIDDEN);
        } else {
            Player player = playerRepository.findByUserName(name);
            if (player != null) {
                return new ResponseEntity<>(makeMap("error", "Name in use"), HttpStatus.CONFLICT);
            } else {
                Player newPlayer = new Player(name, pwd);
                playerRepository.save(newPlayer);
                return new ResponseEntity<>(makeMap("succeed", newPlayer.getId()), HttpStatus.CREATED);
            }
        }
    }

    @Autowired
    private GamePlayerRepository gameplayerRepository;

    @RequestMapping(path = "/games/{gameId}/players", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> joinGame(@PathVariable Long gameId, Authentication authentication) {
        if (authentication != null) {
            Game game = gameRepository.getOne(gameId);
            Player player = getCurrentUser(authentication);
            Date date = new Date();
            if (gameRepository.getOne(gameId) != null) {
                if (gameRepository.getOne(gameId).getGamePlayers().size() == 1) {
                    if (
                            game.getGamePlayers().stream().filter(player1 -> player1.getPlayer().getId() == getCurrentUser(authentication).getId()).findFirst().orElse(null) == null) {
                        GamePlayer gamePlayer = new GamePlayer(player, game, date);
                        gameplayerRepository.save(gamePlayer);
                        return new ResponseEntity<>(makeMap("gpid", gamePlayer.getId()), HttpStatus.CREATED);
                    }
                } else {
                    return new ResponseEntity<>(makeMap("game full", gameId), HttpStatus.FORBIDDEN);
                }
            } else return new ResponseEntity<>(makeMap("no such game", gameId), HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(makeMap("not logged in", gameId), HttpStatus.UNAUTHORIZED);
    }

    @RequestMapping(path = "/game_view/{gpId}", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getGamePlayerData(@PathVariable Long gpId, Authentication authentication) {
        GamePlayer user = gameplayerRepository.findOne(gpId);
        GamePlayer opponent = getOpponent(user);
        if (user.getPlayer().getId() == getCurrentUser(authentication).getId()) {
            Map<String, Object> gameView = new LinkedHashMap<>();
            gameView.put("game", makeGameDTO(user.getGame()));
            gameView.put("gamestate", getGameState(user.getGame()));
            gameView.put("ships", user.getShips()
                    .stream()
                    .map(thisShip -> makeShipDTO(thisShip))
                    .collect(toList()));
            if (user.getGame().getGamePlayers().size() == 2) {
                gameView.put("usersalvoes", getUserSalvos(user));
                gameView.put("userhits", getHits(user));
                gameView.put("usersinks", getSunkShips(user)
                        .stream()
                        .map(ship -> ship.getShipType())
                        .collect(toList()));}else{
                gameView.put("usersalvoes", null);
                gameView.put("userhits", null);
                gameView.put("usersinks", null);
            }
            if (user.getGame().getGamePlayers().size() == 2) {
                gameView.put("enemysalvoes", getEnemySalvos(user));
                gameView.put("enemyhits", getHits(opponent));
                gameView.put("enemysinks", getSunkShips(opponent)
                        .stream()
                        .map(ship -> ship.getShipType())
                        .collect(toList()));
            } else {
                gameView.put("enemysalvoes", null);
                gameView.put("enemyhits", null);
                gameView.put("enemysinks", null);
            }
            return new ResponseEntity(makeMap("gameview", gameView), HttpStatus.OK);

        } else {
            return new ResponseEntity(makeMap("error", "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }
    }

    @Autowired
    ShipRepository shipRepository;

    @RequestMapping(path = "/games/players/{gpId}/ships", method = RequestMethod.POST)
    public ResponseEntity<String> saveShips(@PathVariable Long gpId,
                                            Authentication authentication,
                                            @RequestBody Set<Ship> ships) {
        GamePlayer gamePlayer = gameplayerRepository.findOne(gpId);
        if (gamePlayer.getPlayer().getId() == getCurrentUser(authentication).getId() && getCurrentUser(authentication) != null) {
            if (gamePlayer != null && ships.size() == 5 && gamePlayer.getShips().size() == 0) {
                for (Ship ship : ships) {
                    ship.setGamePlayer(gamePlayer);
                    shipRepository.save(ship);
                }
                return new ResponseEntity("created", HttpStatus.CREATED);
            } else {
                return new ResponseEntity(makeMap("error", "Forbidden"), HttpStatus.FORBIDDEN);
            }
        } else {
            return new ResponseEntity(makeMap("error", "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }
    }

    @Autowired
    SalvoRepository salvoRepository;

    @RequestMapping(path = "/games/players/{gpId}/salvos", method = RequestMethod.POST)
    public ResponseEntity<String> placeSalvoes(@PathVariable Long gpId,
                                               Authentication authentication,
                                               @RequestBody Salvo salvo) {
        GamePlayer gamePlayer = gameplayerRepository.findOne(gpId);
        if (gamePlayer.getPlayer().getId() == getCurrentUser(authentication).getId() || getCurrentUser(authentication) != null) {
            if (gamePlayer != null && salvo.getSalvoLocations().size() == 5 && salvoRepository.findAll().stream().filter(salvorep -> salvorep.getTurn() == salvo.getTurn() && salvorep.getGamePlayer() == gamePlayer ).findFirst().orElse(null) == null) {
                salvo.setGamePlayer(gamePlayer);
                salvoRepository.save(salvo);
                return new ResponseEntity("created", HttpStatus.CREATED);
            }
            else {   return new ResponseEntity(makeMap("error", "Forbidden"), HttpStatus.FORBIDDEN);
            }} else{
            return new ResponseEntity(makeMap("error", "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }}

    @RequestMapping("/leaderboard")
    public Set<Object> getScores() {
        return playerRepository
                .findAll()
                .stream()
                .map(player -> makeScoreDTO(player))
                .collect(toSet());
    }

    private String getWinner(Game game, Boolean sameTurn){
        try{
            GamePlayer loser = game.getGamePlayers().stream().filter(gamePlayer -> getSunkShips(gamePlayer).size() == 5).findFirst().orElse(null);
            GamePlayer winner = getOpponent(loser);
            if (getSunkShips(getOpponent(loser)).size() == 5 && sameTurn == true ){
                Score losing = new Score(loser.getGame(), loser.getPlayer(), 0.5);
                Score winning = new Score(winner.getGame(), winner.getPlayer(), 0.5);
                if(game.getGameOver() == false){
                    saveScores(winning);
                    saveScores(losing);}
                game.setGameOver(true);
                gameRepository.save(game);
                return "tie";
            }else if(sameTurn == true) {
                Score winning = new Score(winner.getGame(), winner.getPlayer(), 1.0);
                if(game.getGameOver() == false){
                    saveScores(winning);}
                game.setGameOver(true);
                gameRepository.save(game);
                return String.valueOf(winner.getId());}
            else return "none, but tried";

        }

        catch(Exception exc) {return "none";}

    }
    public void saveScores(Score score){

        scoreRepository.save(score);

    }

    public Map<String, Object> makeShipDTO(Ship ship) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("shipType", ship.getShipType());
        dto.put("shipLocations", ship.getShipLocations());
        if(ship.getGamePlayer().getGame().getGamePlayers().size() == 2){
            dto.put("sunk", getSinks(ship));}
        return dto;
    }

    public Map<String, Object> makeSalvoDTO(Salvo salvo) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("turn", salvo.getTurn());
        dto.put("location", salvo.getSalvoLocations());
        return dto;
    }


    public Map<String, Object> makeGameDTO(Game game) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        Set<GamePlayer> gameplayerset = game.getGamePlayers();
        dto.put("id", game.getId());
        dto.put("created", game.getDate());
        dto.put("gameplayers", gameplayerset
                .stream()
                .map(gameplayer -> makeGamePlayerDTO(gameplayer))
                .collect(toList()));

        return dto;
    }


    @Autowired
    private ScoreRepository scoreRepository;

    public Map<String, Object> getGameState(Game game){
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        if(game.getGamePlayers().size() == 2 && game.getGamePlayers()
                .stream().mapToInt(gp -> gp.getShips().size()).sum() == 10){
            int leastSalvoes = game.getGamePlayers()
                    .stream().mapToInt(gameplayer -> gameplayer.getSalvoes().size()).min().orElse(-1);

            int mostSalvoes = game.getGamePlayers()
                    .stream().mapToInt(gameplayer -> gameplayer.getSalvoes().size()).max().orElse(-1);
            GamePlayer firstPlayer = gameplayerRepository.findOne(game.getGamePlayers().stream().mapToLong(gamePlayer -> gamePlayer.getId()).min().orElse(-1));
            GamePlayer secondPlayer = gameplayerRepository.findOne(game.getGamePlayers().stream().mapToLong(gamePlayer -> gamePlayer.getId()).max().orElse(-1));
            String winnerName = "none";
            GamePlayer gamePlayerToFire = new GamePlayer();
            if (leastSalvoes == mostSalvoes) {
                gamePlayerToFire = firstPlayer;
            } else {
                gamePlayerToFire = secondPlayer;
            }
            if (game.getGamePlayers().stream()
                    .filter(gamePlayer -> getSunkShips(gamePlayer).size() == 5)
                    .findFirst().orElse(null) != null)
            {
                winnerName = getWinner(game, leastSalvoes == mostSalvoes);
            }
            dto.put("turn", leastSalvoes + 1);
            dto.put("playerToFire", gamePlayerToFire.getId());
            dto.put("winner", winnerName);
            dto.put("gameOver", game.getGameOver());
        }
        else{
            dto.put("state", "waiting for second player");}
        return dto;
    }


    public Object getUserSalvos(GamePlayer gamePlayer){
        return getSalvoes(gamePlayer.getGame().getGamePlayers()
                .stream().filter(player -> player.getId() == gamePlayer.getId()).findFirst().orElse(null));
    }

    public Object getEnemySalvos(GamePlayer gamePlayer) {
        return getSalvoes(gamePlayer.getGame().getGamePlayers()
                .stream().filter(player -> player.getId() != gamePlayer.getId()).findFirst().orElse(null));
    }

    public Map<String, Object> makePlayerDTO(Player player) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", player.getId());
        dto.put("username", player.getUserName());
        return dto;
    }

    public Map<String, Object> makeGamePlayerDTO(GamePlayer gamePlayer) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("id", gamePlayer.getId());
        dto.put("created", gamePlayer.getGameDate());
        dto.put("player", makePlayerDTO(gamePlayer.getPlayer()));
        if (gamePlayer.getScore() != null) {
            dto.put("score", gamePlayer.getScore().getScore());
        }
        return dto;
    }

    public Map<String, Object> makeScoreDTO(Player player) {
        Map<String, Object> dto = new LinkedHashMap<String, Object>();
        dto.put("player", player.getUserName());
        dto.put("totalscore", player.getScores().stream().map(s -> s.getScore()).mapToDouble(s -> s).sum());
        dto.put("wins", player.getScores().stream().filter(s -> s.getScore() == 1.0).count());
        dto.put("losses", player.getScores().stream().filter(s -> s.getScore() == 0.0).count());
        dto.put("ties", player.getScores().stream().filter(s -> s.getScore() == 0.5).count());
        return dto;
    }

    private List<String> shipLocations (GamePlayer gamePlayer) {
        return gamePlayer.getShips()
                .stream()
                .flatMap(s -> s.getShipLocations().stream())
                .collect(toList());
    }

    private List<String> salvoLocations (GamePlayer gamePlayer) {
        return gamePlayer.getSalvoes()
                .stream()
                .flatMap(salvo -> salvo.getSalvoLocations().stream())
                .collect(Collectors.toList());
    }

    private List<String> getHits(GamePlayer gamePlayer){
        GamePlayer opponent = getOpponent(gamePlayer);
        return shipLocations(opponent)
                .stream()
                .filter(cell -> salvoLocations(gamePlayer).contains(cell))
                .collect(toList());
    }

    private boolean getSinks(Ship ship){
        GamePlayer opponent = getOpponent(ship.getGamePlayer());
        return ship.getShipLocations()
                .stream()
                .allMatch(location -> getHits(opponent)
                        .stream()
                        .anyMatch(hit -> hit == location));
    }

    private Set<Ship> getSunkShips(GamePlayer gamePlayer){
        return gamePlayer.getShips().stream().filter(ship -> getSinks(ship) == true).collect(toSet());
    }

    public Player getCurrentUser(Authentication authentication){
        return playerRepository.findByUserName(authentication.getName());
    }


    public Object getSalvoes(GamePlayer gamePlayer) {
        return gamePlayer.getSalvoes().stream()
                .map(salvo -> makeSalvoDTO(salvo))
                .collect(toList());
    }

    private GamePlayer getOpponent(GamePlayer gamePlayer){
        return gamePlayer.getGame().getGamePlayers()
                .stream().filter(gp -> gp != gamePlayer).findFirst().orElse(null);}

    private Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}