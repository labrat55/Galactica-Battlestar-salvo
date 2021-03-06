function allowDrop(ev) {
  ev.preventDefault();
}

function drag(event) {
  if (main.placing) {
    event.dataTransfer.setData("text", event.target.id);
  }
}

function drop(event, el) {
  if (main.placing) {
    event.preventDefault();
    var data = event.dataTransfer.getData("text");
    main.determineLocation(event.target.id, data);
    if (el.firstChild || main.allowed == false) {} else {
      el.appendChild(document.getElementById(data));

    }
  }
}

function show(elementId) {
  document.getElementById("showGalactica").style.display = "none";

  document.getElementById("showCylons").style.display = "none";
  // document.getElementById("id3").style.display="none";
  document.getElementById(elementId).style.display = "block";
  
}
//function myFunction() {
//document.getElementById("showGalactica").style.display = 'block';
//}

var main = new Vue({
  el: '#main',
  data: {
    rows: ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"],
    columns: ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10"],
    gameData: {},
    placing: true,
    gameOver: false,
    timer: setInterval(function () {}, 0),
    message: "",
    status: "",
    numberOfEnemySalvoes: 0,
    turn: 1,
    shipLocations: [],
    usernames: [],
    salvo: [],
    allShots: [],
    locations: [],
    hideTable: false,
    allowed: true,
    shooting: true,
    fireButton: false,
    currentShip: [],
    allShips: {
      
      galactica: {
        shipLength: 4,
        orientation: "horizontal",
        locations: []
      },
      brimir: {
        shipLength: 4,
        orientation: "horizontal",
        locations: []
      },
      pegasus: {
        shipLength: 3,
        orientation: "horizontal",
        locations: []
      },
      viper: {
        shipLength: 2,
        orientation: "horizontal",
        locations: []
      },
      raptor: {
        shipLength: 2,
        orientation: "horizontal",
        locations: []
      },
      cylonBaseStar: {
        shipLength: 4,
        orientation: "horizontal",
        locations: []
      },
      baseStar: {
        shipLength: 4,
        orientation: "horizontal",
        locations: []
      },
      surtur: {
        shipLength: 3,
        orientation: "horizontal",
        locations: []
      },
      liche: {
        shipLength: 2,
        orientation: "horizontal",
        locations: []
      },
      raider: {
        shipLength: 2,
        orientation: "horizontal",
        locations: []
      }
    },

  },


  methods: {

    getDataObject: function (gamePlayer) {
      console.log("fetching")
      var fetchConfig =
        fetch("/api/game_view/" + gamePlayer, {
          method: "GET",
          credential: "include"
        }).then(
          r => {
            if (r.status == 401) {
              this.peekingPlayer()
            } else if (r.status == 200) {
              this.onDataFetched(r)
            }
          },
        )
    },
    onConversionToJsonSuccessful: function (json) {
      main.gameData = json;
      console.log(this.gameData)
      this.getShips();
      this.getPlayers();
      if (this.gameData.gameview.ships.length !== 5) {
        this.placing = true
      } else {
        this.placing = false
      }
      if (this.gameData.gameview.gamestate.gameOver == true) {
        this.placing = false
        this.gameOver()

      }
      if (this.gameData.gameview.gamestate.state !== "waiting for second player" && this.gameData.gameview.gamestate.playerToFire.toString() == this.determineGamePlayer() && this.gameOver !== true) {
        this.StopCheckForServerData()
        this.getShips();
        this.getPlayers();
      } else if (this.shooting == true) {
        this.StartCheckServerForData()
      }
      if (this.gameData.gameview.usersalvoes != null) {
        this.getSalvoes(this.gameData.gameview.usersalvoes, this.gameData.gameview.userhits, "salvo ")
      }
      if (this.gameData.gameview.enemysalvoes != null) {
        this.getSalvoes(this.gameData.gameview.enemysalvoes, this.gameData.gameview.enemyhits, "ship ")
      }

    },
    onDataFetched: function (response) {
      response.json()
        .then(this.onConversionToJsonSuccessful)
    },
    StartCheckServerForData: function () {
      this.timer = setInterval(function () {
        main.getDataObject(main.determineGamePlayer())
      }, 2000)
      this.waitingForOtherPlayer()
    },
    StopCheckForServerData: function () {
      clearInterval(this.timer)
      this.playerToFire()
    },

    determineGamePlayer: function () {
      var url = location.search;
      var x = url.split('=')[1];
      this.gamePlayer = x;
      return this.gamePlayer
    },
    waitingForOtherPlayer: function () {
      document.getElementById("salvoTableTotal").style.opacity = "0.5"
      this.shooting = false
      this.message = "Waiting for (OPPONENT)"
    },
    playerToFire: function () {
      document.getElementById("salvoTableTotal").style.opacity = "1"
      this.shooting = true;
      clearInterval(this.timer);
      console.log("clear timer");
    },
    gameOver: function () {
      document.getElementById("salvoTableTotal").style.opacity = "0.5"
      this.shooting = false
      this.gameOver = true
      clearInterval(this.timer)
      if (this.gameData.gameview.gamestate.winner == this.determineGamePlayer()) {
        this.status = "What an ace! You Win!"
      } else if (this.gameData.gameview.gamestate.winner == "tie") {
        this.status = "It's a tie!"
      } else {
        this.status = "Game Over! You Lose!"
      }
    },

    getShips: function () {
      for (i = 0; i < this.gameData.gameview.ships.length; i++) {
        let ship = this.gameData.gameview.ships
        currentShip = eval("this.allShips." + ship[i].shipType)
        if (currentShip.locations.length == 0) {
          for (j = 0; j < ship[i].shipLocations.length; j++) {
            currentShip.locations.push(ship[i].shipLocations[j])
          }
        }
        let cell = document.getElementById("ship " + currentShip.locations[0])
        if (cell.hasChildNodes() == false) {
          let elem = document.getElementById(ship[i].shipType);
          elem.style.display = "block"
          if (currentShip.locations[0][0] == currentShip.locations[1][0]) {
            elem.className = "normal";
            currentShip.orientation = "horizontal"
          } else {
            elem.className = "rotate";
            currentShip.orientation = "vertical"
          }
          cell.appendChild(elem);
        }
      }
    },
    getSalvoes: function (salvoType, hitType, grid) {
      for (i = 0; i < salvoType.length; i++) {
        for (j = 0; j < salvoType[i].location.length; j++) {
          let shot = salvoType[i].location[j];
          let cell = document.getElementById(grid + shot);
          if (hitType.includes(shot)) {
            cell.className += " hit";
            cell.style.backgroundColor = ""
            if (cell.firstChild && cell.childElementCount < 2) {
              if (cell.firstChild.className == "rotate" || cell.firstChild.className == "normal") {
                let elem = document.createElement("img");
                elem.src = ""
                cell.appendChild(elem)
                elem.className = ""
              }
            } else if (cell.childElementCount < 2) {
              let elem = document.createElement("img");
              elem.src = ""
              cell.appendChild(elem)
              elem.className = ""
            }
          } else if (!shot == "../assets/red_cross.png") {
            cell.className = " miss"
          }
        }
      }
      this.numberOfEnemySalvoes = this.gameData.gameview.enemysalvoes.length
    },
    getPlayers: function () {
      let players = this.gameData.gameview.game.gameplayers
      for (i = 0; i < players.length; i++) {
        if (players[i].id == this.id) {
          this.usernames.push(players[i].player.username + " (You)")
        } else {
          this.usernames.push(players[i].player.username)
        }
      }
    },
    logout: function () {
      fetch("/api/logout")
        .then(window.location.href = '/web/games.html')
        .catch();
    },
        noCheating: function () {
          document.getElementById("main").innerHTML = ""
          document.getElementById("main").innerHTML = "<h1>No Cheating!!</h1>"
        },
    determineLocation: function (location, ship) {
      this.allowed = true
      let shipName = eval("this.allShips." + ship)
      let shipLength = shipName.shipLength
      let locations = []
      if (shipName.orientation === "horizontal") {
        for (i = location.slice(6); i < (parseInt(location.slice(6)) + shipLength); i++) {
          if (i < 11) {

            this.checkLocation((location[5] + i), ship)

          } else {

            this.allowed = false
          }
          if (this.allowed == true) {
            locations.push(location[5] + i)
          }
        }
      }
      if (shipName.orientation === "vertical") {
        let rowNumber = 0
        for (i = 0; i < this.rows.length; i++) {
          if (location[5] == this.rows[i]) {
            rowNumber = i
          }

        }
        for (i = 0; i < shipLength; i++) {
          if (rowNumber < 10) {
            this.checkLocation((this.rows[rowNumber] + location.slice(6)), ship)
          } else {

            this.allowed = false
          }
          if (this.allowed == true) {
            locations.push(this.rows[rowNumber] + location.slice(6));
            rowNumber++
          }
        }
      }
      if (this.allowed == true) {
        shipName.locations = locations
      }
    },
    checkLocation: function (loc, ship) {
      for (vessel in this.allShips) {
        checkShip = eval("this.allShips." + vessel + ".locations")
        if (vessel !== ship) {
          if (checkShip.includes(loc)) {
            this.allowed = false;
            if (cells.length == 5) {
              this.fireButton = true;
            } else {
              this.fireButton = false;
            }

          }
        }
      }
    },
    rotateShip: function (shipId) {
      if (this.placing) {
        var element = document.getElementById(shipId);
        var ship = eval("this.allShips." + shipId)
        var parent = document.getElementById(shipId).parentNode.id
        var container = document.getElementById(shipId).parentNode.className
        if (container != "shipcontainer") {
          if (element.className === "normal") {
            ship.orientation = "vertical";
            this.determineLocation(parent, shipId)
            if (this.allowed == true) {
              element.className = "rotate"
            } else {
              ship.orientation = "horizontal"
            }
          } else if (element.className === "rotate") {
            ship.orientation = "horizontal"
            this.determineLocation(parent, shipId)
            if (this.allowed == true) {
              element.className = "normal"
            } else {
              ship.orientation = "vertical"
            }
          }
        }
      }
    },
    makeShipJSON: function () {
      shipList = []
      for (vessel in this.allShips) {
        locations = eval("this.allShips." + vessel + ".locations")
        if (locations.length > 0) {
          ship = {}
          ship.shipType = vessel
          ship.shipLocations = locations
          shipList.push(ship)
        } else {
          alert("Please place only 5 crafts from the launch pad!");
          break
        }
      }
      this.sendShips(shipList, this.determineGamePlayer())
    },
    sendShips: function (ships, gamePlayer) {
      fetch("/api/games/players/" + gamePlayer + "/ships", {
          credentials: 'include',
          method: 'POST',
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(ships)
        })
        .then(response => console.log(response))
        .then(r => {
          this.placing = false;
          this.getDataObject(this.determineGamePlayer())
        })
    },
    sendSalvo: function () {
      if (this.salvo.length == 5) {
        salvo = {
          turn: this.gameData.gameview.gamestate.turn,
          salvoLocations: this.salvo

        }
        fetch("/api/games/players/" + this.determineGamePlayer() + "/salvos", {
            credentials: 'include',
            method: 'POST',
            headers: {
              'Accept': 'application/json',
              'Content-Type': 'application/json'
            },
            body: JSON.stringify(salvo)
          }).then(response => console.log(response))
          .then(this.salvo = [])
          .then(r => {
            this.getDataObject(main.determineGamePlayer())
          })
      } else {
        alert("Fire 5 shots or wait for opponent!")
      }
    },
    placeShot: function (shotCell) {
      if (this.gameData.gameview.gamestate.playerToFire.toString() == this.determineGamePlayer()) {
        if (!this.allShots.includes(shotCell) || this.salvo.includes(shotCell)) {
          if (!this.salvo.includes(shotCell)) {
            if (this.salvo.length < 5) {
              document.getElementById("salvo " + shotCell).className += " shot"
              this.salvo.push(shotCell);
              this.allShots.push(shotCell)
            } else {
              alert("Check your round for 5 shots!")
            }
          } else {
            this.salvo = this.salvo.filter(e => e !== shotCell);
            this.allShots = this.allShots.filter(e => e !== shotCell);
            document.getElementById("salvo " + shotCell).className = "salvoTable"
          }
        }
      }
    }
  },
  created: function () {
    this.getDataObject(this.determineGamePlayer())
  }
})

//============== special audio playback ====================

$(document).ready(function () {
  $(".toggle").click(function () {
    $(this).toggleClass("active");
  });

  $(".slide-body").click(function () {
    $(".slide-body .slide").toggleClass("active");
  });

  $(".on").click(function () {
    $("#audio-player")[0].play();
  });

  $(".off").click(function () {
    $("#audio-player")[0].pause();
  });
});
