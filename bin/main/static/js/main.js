// 검색 버튼 클릭 이벤트
document.addEventListener("DOMContentLoaded", function () {
    const searchBtn = document.getElementById("searchBtn");
    const searchInput = document.getElementById("searchInput");

    if (searchBtn) {
        searchBtn.addEventListener("click", function () {
            const keyword = searchInput.value.trim();
            if (keyword) {
                alert("검색 기능은 추후 구현 예정입니다.\n검색어: " + keyword);
            } else {
                alert("검색어를 입력하세요!");
            }
        });
    }
});
