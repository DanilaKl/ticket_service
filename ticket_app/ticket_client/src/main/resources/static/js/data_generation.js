function getRandomInInterval(start, end) {
    return Math.round(Math.random() * (end - start)) + start;
}

function getRandomDate() {
    const  year = new Date().getFullYear() + 1;
    const  month = String(getRandomInInterval(1, 12)).padStart(2, '0');
    const  day = String(getRandomInInterval(1, 28)).padStart(2, '0');
    const  hours = String(getRandomInInterval(0, 23)).padStart(2, '0');
    const  minutes = String(getRandomInInterval(0, 5) * 10).padStart(2, '0');

    return `${day}.${month}.${year}, ${hours}:${minutes}`;
}

function getRandomTickets() {
    const numberCount = getRandomInInterval(1, 3);
    const letterCount = getRandomInInterval(1, Math.round(5 / numberCount));

    const result = [];
    const startCharCode = 'A'.charCodeAt(0);

    for (let i = 0; i < letterCount; i++) {
        const letter = String.fromCharCode(startCharCode + i);

        for (let j = 1; j <= numberCount; j++) {
            result.push(`${letter}${j}`);
        }
    }

    return result.join(', ');
}

function getRandomEventName() {
    return 'Event' + String(getRandomInInterval(1, 10_000)).padStart(5, '0');
}

document.addEventListener('click', (e) => {
    if (e.target.classList.contains('generate-btn')) {
        const type = e.target.dataset.type;
        const form = e.target.closest('form');
        if (type === 'event') {
            form.querySelector('[name="name"]').value = getRandomEventName();
            form.querySelector('[name="dateTime"]').value = getRandomDate();
            form.querySelector('[name="tickets"]').value = getRandomTickets();
        } else if (type === 'user') {
            form.querySelector('[name="userId"]').value = crypto.randomUUID();
        }
    }
});