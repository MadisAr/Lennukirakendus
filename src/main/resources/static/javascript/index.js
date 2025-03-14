// loob formi valiku asukoha kohta
function createNewFormEntry(element, timeForm, document) {
  const newOption = document.createElement("option");
  newOption.value = element;
  newOption.textContent = element;
  timeForm.appendChild(newOption);
}

// loob istmete skeemi, tähistades võetud ja soovitatud kohad
function createSeatsScheme(takenSeats, recommendedSeats) {
  document.getElementById("seat-selection").classList.remove("hidden");
  const seatMap = document.querySelector(".seat-map");
  seatMap.innerHTML = "";

  // luuakse 20 rida, igaühes 6 kohta
  for (let i = 1; i <= 20; i++) {
    const row = document.createElement("div");
    row.className = "d-flex mb-2";

    for (let j = 1; j <= 6; j++) {
      const seat = document.createElement("button");
      const seatName = `${i}${String.fromCharCode(64 + j)}`;

      seat.type = "button";
      seat.textContent = seatName;
      if (takenSeats.includes(seatName)) {
        seat.disabled = true;
        seat.className = "btn btn-secondary seat mx-1"; // võetud koht
      } else {
        seat.className = "btn btn-outline-primary seat mx-1"; // vaba koht

        // lisab klikimise kuulaja, et muuta koha olek
        seat.addEventListener("click", function () {
          if (this.classList.contains("btn-success")) {
            this.classList.remove("btn-success");
            removeSelectedSeats(this.textContent); // eemaldab valitud koha
          } else {
            this.classList.add("btn-success");
            addSelectedSeats(this.textContent); // lisab valitud koha
          }
        });

        // kui koht on soovitatud, tehakse see automaatselt valituks
        if (recommendedSeats.includes(seatName)) {
          seat.click();
        }
      }
      row.appendChild(seat);
      if (j == 3) {
        const gap = document.createElement("div");
        gap.style.width = "20px";
        row.appendChild(gap);
      }
    }

    seatMap.appendChild(row);
  }
}

// eemaldab valitud koha valitud kohtade loendist
function removeSelectedSeats(seatNr) {
  const selectedSeatsInput = document.getElementById("selected-seats");
  let selectedSeats = selectedSeatsInput.value
    ? selectedSeatsInput.value.split(",")
    : [];

  if (selectedSeats.includes(seatNr)) {
    selectedSeatsInput.value = selectedSeats
      .filter((val) => val != seatNr)
      .join(",");
  }
}

// lisab valitud koha valitud kohtade loendisse
// teoriaas saaks siis inputi saata submitimisel post requestina serverile,
// aga hetkel ma seda funktsionaalsust ei lisanud, sest niikuinii ei tee backend hetkel midagi antud andmetega
function addSelectedSeats(seatNr) {
  const selectedSeatsInput = document.getElementById("selected-seats");
  let selectedSeats = selectedSeatsInput.value
    ? selectedSeatsInput.value.split(",")
    : [];

  if (!selectedSeats.includes(seatNr)) {
    selectedSeats.push(seatNr);
  }

  selectedSeatsInput.value = selectedSeats.join(",");
}

// teeb get requesti soovitatud kohtade saamiseks
async function getRecommendedSeats(flightId, nr, windowSeat, legRoom, aisle) {
  console.log(`/api/flights/recommendedSeats?id=${flightId}&nr=${nr}&windowSeat=${windowSeat}&legRoom=${legRoom}&aisle=${aisle}`)
  const res = await fetch(
    `/api/flights/recommendedSeats?id=${flightId}&nr=${nr}&windowSeat=${windowSeat}&legRoom=${legRoom}&aisle=${aisle}`
  );
  const data = await res.json();
  return data;
}

// teeb get requesti võetud kohtade saamiseks
async function getTakenSeats(flightId) {
  const res = await fetch(`/api/flights/takenSeats?id=${flightId}`);
  const data = await res.json();
  return data;
}

// loob uue lennu kohta tabeli elemendi
function createNewTableEntry(flight, flightsTableBody) {
  const dateInfo = flight.flight_date.split(" ");
  const arrivalDateInfo = flight.arrival_date.split(" ");

  const row = document.createElement("tr");
  row.innerHTML = `
    <td>${dateInfo[0]}</td>
    <td>${dateInfo[1]}</td>
    <td>${arrivalDateInfo[0]}</td>
    <td>${arrivalDateInfo[1]}</td>
    <td>${flight.departure_airport}</td>
    <td>${flight.destination_airport}</td>
    <td>${flight.price}</td>
    <td>
    <button class="displaySeatOptions">Kohtade valikud</button>
    </td>
  `;

  const row2 = document.createElement("tr");
  row2.classList.add("hidden");
  row2.innerHTML = `
    <td>
      <label>
        <input type="checkbox" id="leg-room-preference">
        Eelistan rohkem jalaruumi
      </label>
    </td>
    <td>
      <label>
        <input type="checkbox" id="window-seat-preference">
        Eelistan aknakohta
      </label>
    </td>
    <td>
      <label>
        <input type="checkbox" id="aisle-seat-preference">
        Eelistan vahekäigu juurde
      </label>
    </td>
    <td>
    <label>
      kohtade arv:
      <input type="number" min="1" max="10" value="1" class="seat-count">
    </label>
    </td>
    <td>
      <button class="btn btn-success buy-button" value="${flight.id}">Vaata kohti</button>
    </td>
  `;

  flightsTableBody.appendChild(row);
  flightsTableBody.appendChild(row2);

  // kuulab osta-nupu klikkimist ja laadib istmete skeemi
  row2
    .querySelector(".buy-button")
    .addEventListener("click", async function () {
      const seatCount = row2.querySelector(".seat-count").value;
      const windowSeat = row2.querySelector("#window-seat-preference").checked;
      const legRoom = row2.querySelector("#leg-room-preference").checked;
      const aisle = row2.querySelector("#aisle-seat-preference").checked;

      if (seatCount < 1 || isNaN(seatCount)) {
        seatCount = 1;
      } else if (seatCount > 10) {
        seatCount = 10;
      }

      const flightId = this.value;
      const taken = await getTakenSeats(flightId);
      const recommendedSeats = await getRecommendedSeats(
        flightId,
        seatCount,
        windowSeat, legRoom, aisle
      );
      createSeatsScheme(taken, recommendedSeats);
    });

  row
    .querySelector(".displaySeatOptions")
    .addEventListener("click", function () {
      row2.classList.contains("hidden")
        ? row2.classList.remove("hidden")
        : row2.classList.add("hidden");
    });

}

// käivitatakse lehe laadimisel, et saada sihtkohad
document.addEventListener("DOMContentLoaded", function () {
  const timeForm = document.getElementById("destination");
  const flightsForm = document.getElementById("flight-filter-form");
  const flightsTableBody = document.querySelector("#flights tbody");
  const noResultsText = document.getElementById("no-results");

  fetch("/api/flights/destinations")
    .then((res) => res.json())
    .then((res) => {
      // lisa iga saadavaloleva lennu kohta formi uus valik
      res.forEach((element) => {
        createNewFormEntry(element, timeForm, document);
      });
    })
    .catch((err) => console.log(err));

  // kuulab formi esitamist ja teeb lennuotsingu
  flightsForm.addEventListener("submit", function (event) {
    event.preventDefault();
    flightsTableBody.innerHTML = "";
    noResultsText.classList.add("hidden");
    const params = new URLSearchParams();

    const destination = document.getElementById("destination").value;
    const date = document.getElementById("date").value;
    const time = document.getElementById("time").value;
    const minPrice = document.getElementById("min-price").value;
    const maxPrice = document.getElementById("max-price").value;

    if (date) params.append("flight_date", date);
    if (minPrice) params.append("min_price", minPrice);
    if (maxPrice) params.append("max_price", maxPrice);
    if (time) params.append("time", time);
    if (destination) params.append("destination_airport", destination);

    const queryString = params.toString();
    const url = `/api/flights${queryString ? "?" + queryString : "/all"}`;
    fetch(url)
      .then((res) => {
        return res.json();
      })
      .then((res) => {
        if (res.length == 0) {
          noResultsText.classList.remove("hidden");
        } else {
          res.forEach((element) => {
            createNewTableEntry(element, flightsTableBody);
          });
        }
      })
      .catch((err) => console.log(err));
  });
});
