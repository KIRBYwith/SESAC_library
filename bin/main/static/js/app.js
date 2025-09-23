// /js/app.js

// 공통: 간단한 API 헬퍼
async function api(path, { method = "GET", body, headers = {} } = {}) {
    const opts = {
        method,
        headers: { "Content-Type": "application/json", ...headers },
        credentials: "include", // 세션/쿠키 기반이면 include
    };
    if (body) opts.body = JSON.stringify(body);

    const res = await fetch(path, opts);
    // 필요 시 에러 처리
    if (!res.ok) throw new Error(`API ${method} ${path} -> ${res.status}`);
    return res.headers.get("content-type")?.includes("application/json")
        ? res.json()
        : res.text();
}

// 로그인 폼 처리
const loginForm = document.getElementById("loginForm");
if (loginForm) {
    loginForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const form = new FormData(loginForm);
        const email = form.get("email");
        const password = form.get("password");
        const msg = document.getElementById("loginMsg");

        try {
            await api("/api/auth/login", { method: "POST", body: { email, password } });
            msg.textContent = "로그인 성공! 곧 이동합니다…";
            window.location.href = "/books.html";
        } catch (err) {
            msg.textContent = "로그인 실패: " + err.message;
            msg.classList.add("text-danger");
        }
    });
}

// 도서 목록 렌더링
async function renderBooks() {
    const listEl = document.getElementById("bookList");
    if (!listEl) return;

    const q = document.getElementById("q");
    const query = q?.value?.trim() ?? "";
    const data = await api(`/api/books${query ? `?q=${encodeURIComponent(query)}` : ""}`);

    listEl.innerHTML = "";
    (data || []).forEach((book) => {
        const col = document.createElement("div");
        col.className = "col-12 col-md-6 col-lg-4";
        col.innerHTML = `
      <div class="card h-100 shadow-sm">
        <div class="card-body">
          <h5 class="card-title">${escapeHTML(book.title)}</h5>
          <p class="card-subtitle text-muted mb-2">${escapeHTML(book.author)}</p>
          <p class="card-text small">${escapeHTML(book.description ?? "")}</p>
          <button class="btn btn-sm btn-primary" data-id="${book.id}">대출하기</button>
        </div>
      </div>
    `;
        listEl.appendChild(col);
    });

    // 버튼 핸들러
    listEl.querySelectorAll("button[data-id]").forEach((btn) =>
        btn.addEventListener("click", async () => {
            const id = btn.getAttribute("data-id");
            try {
                await api(`/api/loans`, { method: "POST", body: { bookId: id } });
                alert("대출 성공");
            } catch (e) {
                alert("대출 실패: " + e.message);
            }
        })
    );
}

// 검색 입력 연동
const qInput = document.getElementById("q");
if (qInput) {
    qInput.addEventListener("input", debounce(renderBooks, 300));
    renderBooks();
}

// 유틸: 간단한 XSS 방지 이스케이프 (모의해킹용으로 일부 페이지에서 의도적으로 제거해볼 수도 있음)
function escapeHTML(str) {
    return String(str ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

// 유틸: 디바운스
function debounce(fn, ms) {
    let t;
    return (...args) => {
        clearTimeout(t);
        t = setTimeout(() => fn.apply(null, args), ms);
    };
}
