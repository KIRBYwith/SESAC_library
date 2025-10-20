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
	logout,
	updateUserInfo,
	checkAuthStatus,
};

// API 관련 함수들
async function logout() {
	if (confirm('정말 로그아웃 하시겠습니까?')) {
		try {
			const response = await fetch('/api/auth/logout', {
				method: 'POST'
			});

			if (response.ok) {
				localStorage.removeItem('isLoggedIn');
				localStorage.removeItem('user');
				alert('로그아웃 되었습니다.');
				location.reload();
			} else {
				throw new Error('로그아웃 처리 중 오류가 발생했습니다.');
			}
		} catch (error) {
			console.error('로그아웃 오류:', error);
			localStorage.removeItem('isLoggedIn');
			localStorage.removeItem('user');
			alert('로그아웃 되었습니다.');
			location.reload();
		}
	}
}

// 사용자 정보 업데이트 (서버에서 최신 정보 가져오기)
async function updateUserInfo() {
	try {
		const response = await fetch('/api/auth/me');
		const userData = await response.json();

		if (response.ok && userData.authenticated) {
			localStorage.setItem('isLoggedIn', 'true');
			localStorage.setItem('user', JSON.stringify(userData));
			return userData;
		} else {
			localStorage.removeItem('isLoggedIn');
			localStorage.removeItem('user');
			return null;
		}
	} catch (error) {
		console.error('사용자 정보 업데이트 오류:', error);
		return null;
	}
}

// 로그인 상태 확인 및 헤더 업데이트
function checkAuthStatus() {
	const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';
	const loginBtn = document.getElementById('loginBtn');
	const signupBtn = document.getElementById('signupBtn');
	const mypageBtn = document.getElementById('mypageBtn');
	const adminBtn = document.getElementById('adminBtn');
	const logoutBtn = document.getElementById('logoutBtn');

	if (isLoggedIn) {
		const user = JSON.parse(localStorage.getItem('user') || '{}');

		// 로그인된 상태
		if (loginBtn) loginBtn.style.display = 'none';
		if (signupBtn) signupBtn.style.display = 'none';
		if (mypageBtn) mypageBtn.style.display = 'inline-block';
		if (logoutBtn) logoutBtn.style.display = 'inline-block';

		// 관리자 체크
		if (user.role === 'ADMIN' && adminBtn) {
			adminBtn.style.display = 'inline-block';
		}
	} else {
		// 로그인되지 않은 상태
		if (loginBtn) loginBtn.style.display = 'inline-block';
		if (signupBtn) signupBtn.style.display = 'inline-block';
		if (mypageBtn) mypageBtn.style.display = 'none';
		if (adminBtn) adminBtn.style.display = 'none';
		if (logoutBtn) logoutBtn.style.display = 'none';
	}
}


