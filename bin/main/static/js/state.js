// Simple local state management using localStorage
// Keys used:
// - user: { email, name, role, points, loans: number[], reservations: number[] }
// - bookStatus: { [bookId:number]: { status: "대출가능"|"대출중", loanedBy?: string } }
// - reservationQueues: { [bookId:number]: string[] } // array of user emails
// - exchangeRequests: { id, email, name, pointsUsed, createdAt, status }

function readJSON(key, fallback) {
	try {
		const raw = localStorage.getItem(key);
		return raw ? JSON.parse(raw) : fallback;
	} catch (e) {
		return fallback;
	}
}

function writeJSON(key, value) {
	localStorage.setItem(key, JSON.stringify(value));
}

function getCurrentUser() {
	return readJSON("user", null);
}

function setCurrentUser(user) {
	writeJSON("user", user);
	localStorage.setItem("isLoggedIn", user ? "true" : "false");
}

function ensureBookState(bookIds) {
	const state = readJSON("bookStatus", {});
	(bookIds || []).forEach((id) => {
		if (!state[id]) state[id] = { status: "대출가능" };
	});
	writeJSON("bookStatus", state);
	return state;
}

function getBookState() {
	return readJSON("bookStatus", {});
}

function setBookLoan(bookId, email) {
	const state = getBookState();
	state[bookId] = { status: "대출중", loanedBy: email };
	writeJSON("bookStatus", state);
}

function clearBookLoan(bookId) {
	const state = getBookState();
	state[bookId] = { status: "대출가능" };
	writeJSON("bookStatus", state);
}

function getReservationQueues() {
	return readJSON("reservationQueues", {});
}

function addReservation(bookId, email) {
	const queues = getReservationQueues();
	if (!queues[bookId]) queues[bookId] = [];
	if (!queues[bookId].includes(email)) queues[bookId].push(email);
	writeJSON("reservationQueues", queues);
}

function popNextReservation(bookId) {
	const queues = getReservationQueues();
	const next = (queues[bookId] || []).shift();
	writeJSON("reservationQueues", queues);
	return next || null;
}

function awardPoints(user, delta) {
	const u = user || getCurrentUser();
	if (!u) return null;
	u.points = (u.points || 0) + delta;
	setCurrentUser(u);
	return u.points;
}

function getExchangeRequests() {
	return readJSON("exchangeRequests", []);
}

function addExchangeRequest(email, name, pointsUsed) {
	const list = getExchangeRequests();
	const id = Date.now();
	list.push({ id, email, name, pointsUsed, createdAt: new Date().toISOString(), status: "pending" });
	writeJSON("exchangeRequests", list);
	return id;
}

function updateExchangeRequest(id, updates) {
	const list = getExchangeRequests();
	const idx = list.findIndex((r) => r.id === id);
	if (idx !== -1) {
		list[idx] = { ...list[idx], ...updates };
		writeJSON("exchangeRequests", list);
	}
}

window.State = {
	readJSON,
	writeJSON,
	getCurrentUser,
	setCurrentUser,
	ensureBookState,
	getBookState,
	setBookLoan,
	clearBookLoan,
	getReservationQueues,
	addReservation,
	popNextReservation,
	awardPoints,
	getExchangeRequests,
	addExchangeRequest,
	updateExchangeRequest,
};


