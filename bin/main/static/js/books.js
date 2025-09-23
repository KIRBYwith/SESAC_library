// 더미 도서 데이터
const books = [
    { title: "자바 웹 개발", author: "홍길동" },
    { title: "스프링 부트 입문", author: "김철수" },
    { title: "자바스크립트 200제", author: "이영희" }
];

// URL 파라미터에서 검색어 가져오기
const params = new URLSearchParams(window.location.search);
const keyword = params.get("keyword");

document.addEventListener("DOMContentLoaded", () => {
    const resultList = document.getElementById("result-list");

    if (!keyword) {
        resultList.innerHTML = "<li>검색어를 입력하세요.</li>";
        return;
    }

    const filtered = books.filter(b =>
        b.title.includes(keyword) || b.author.includes(keyword)
    );

    if (filtered.length === 0) {
        resultList.innerHTML = `<li>"${keyword}" 에 대한 검색 결과가 없습니다.</li>`;
    } else {
        filtered.forEach(b => {
            const li = document.createElement("li");
            li.textContent = `${b.title} - ${b.author}`;
            resultList.appendChild(li);
        });
    }
});
