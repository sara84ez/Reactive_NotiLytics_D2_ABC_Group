
// NotiLytics D2 - WebSocket Client (Modern, JSON-based, Dark Mode Compatible)

let ws;

function connectWS() {
    ws = new WebSocket("/ws");

    ws.onopen = () => {
        console.log("WebSocket connected.");
    };

    ws.onmessage = (event) => {
        try {
            const data = JSON.parse(event.data);

            if (data.articles) {
                appendArticles(data.articles);
            }

            if (data.sources) {
                appendSources(data.sources);
            }
        } catch (e) {
            console.error("Invalid JSON:", event.data);
        }
    };

    ws.onclose = () => console.log("WebSocket closed.");
    ws.onerror = (err) => console.error("WebSocket error:", err);
}

function sendSearchQuery(q) {
    ws.send(JSON.stringify({
        type: "search",
        query: q
    }));
}

function sendSourceRequest(country, category, language) {
    ws.send(JSON.stringify({
        type: "sources",
        country,
        category,
        language
    }));
}

function appendArticles(list) {
    const div = document.getElementById("articles-container");
    if (!div) return;

    list.forEach(a => {
        const card = `
            <div class="card bg-dark text-light border-secondary mb-3">
                <div class="card-body">
                    <h5 class="card-title">${a.title}</h5>
                    <p class="card-text">${a.description || ""}</p>
                    <a href="${a.url}" class="btn btn-outline-info" target="_blank">Open</a>
                </div>
            </div>`;
        div.innerHTML += card;
    });
}

function appendSources(list) {
    const div = document.getElementById("sources-container");
    if (!div) return;

    div.innerHTML = "";
    list.forEach(s => {
        const card = `
            <div class="card bg-dark text-light border-secondary mb-3">
                <div class="card-body">
                    <h5 class="card-title">${s.name}</h5>
                    <p>${s.description || ""}</p>
                    <a href="/source/${s.id}" class="btn btn-outline-warning">View</a>
                </div>
            </div>`;
        div.innerHTML += card;
    });
}

window.onload = connectWS;
