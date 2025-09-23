// 숫자 포맷팅 함수 (약 0만, 약 0십만 형태)
function formatNumber(num) {
    if (num >= 100000000) { // 1억 이상
        return '약 ' + Math.floor(num / 100000000) + '억';
    } else if (num >= 10000000) { // 1천만 이상
        return '약 ' + Math.floor(num / 10000000) + '천만';
    } else if (num >= 1000000) { // 100만 이상
        return '약 ' + Math.floor(num / 1000000) + '백만';
    } else if (num >= 100000) { // 10만 이상
        return '약 ' + Math.floor(num / 100000) + '십만';
    } else if (num >= 10000) { // 1만 이상
        return '약 ' + Math.floor(num / 10000) + '만';
    } else if (num >= 1000) { // 1천 이상
        return '약 ' + Math.floor(num / 1000) + '천';
    } else {
        return num.toString();
    }
}

// 페이지 로드 시 숫자 포맷팅 적용
document.addEventListener('DOMContentLoaded', function() {
    // 모든 통계 숫자들 포맷팅
    const statNumbers = document.querySelectorAll('.stat-number');
    statNumbers.forEach(element => {
        const text = element.textContent;
        const number = parseInt(text.replace(/,/g, ''));
        if (!isNaN(number) && number > 0) {
            element.textContent = formatNumber(number);
        }
    });

    // 다른 숫자 표시 요소들도 포맷팅 (클래스명이 다른 경우)
    const numberElements = document.querySelectorAll('[data-format-number]');
    numberElements.forEach(element => {
        const text = element.textContent;
        const number = parseInt(text.replace(/,/g, ''));
        if (!isNaN(number) && number > 0) {
            element.textContent = formatNumber(number);
        }
    });
});
