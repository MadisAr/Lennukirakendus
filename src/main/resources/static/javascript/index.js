function createNewFormEntry(element, timeForm, document) {
  const newOption = document.createElement("option");
  newOption.value = element;
  newOption.textContent = element;
  timeForm.appendChild(newOption);
}

function createSeatsScheme(takenSeats) {
  document.getElementById("seat-selection").classList.remove("hidden")
  const seatMap = document.querySelector(".seat-map");
  seatMap.innerHTML = "";

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
        seat.className = "btn btn-secondary seat mx-1";
      } else {
        seat.className = "btn btn-outline-primary seat mx-1";

        seat.addEventListener("click", function () {
          this.classList.toggle("btn-success");
          updateSelectedSeats();
        });
      }

      row.appendChild(seat);
    }

    seatMap.appendChild(row);
  }
}

function openSeatPlan(seats, flightId) {
  fetch(`/api/flights/takenSeats?id=${flightId}`)
    .then((res) => res.json())
    .then((res) => {
      createSeatsScheme(res);
    })
    .catch((err) => {
      console.log(err);
    });
}

function createNewTableEntry(flight, flightsTableBody) {
  const row = document.createElement("tr");
  const dateInfo = flight.flight_date.split(" ");
  const arrivalDateInfo = flight.arrival_date.split(" ");

  row.innerHTML = `
    <td>${dateInfo[0]}</td>
    <td>${dateInfo[1]}</td>
    <td>${arrivalDateInfo[0]}</td>
    <td>${arrivalDateInfo[1]}</td>
    <td>${flight.departure_airport}</td>
    <td>${flight.destination_airport}</td>
    <td>${flight.price}</td>
    <td>
      <input type="number" min="1" max="10" value="1" class="seat-count">
    </td>
    <td>
      <button class="btn btn-success buy-button" value="${flight.id}">Buy</button>
    </td>
  `;

  row.querySelector(".buy-button").addEventListener("click", function () {
    const seats = row.querySelector(".seat-count").value;
    const flightId = this.value;
    openSeatPlan(seats, flightId);
  });

  flightsTableBody.appendChild(row);
}

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
