function AccountCurrencySummary({ summary }) {
    const defaultSummary = {
        RON: {
            totalBalance: 0,
            accountCount: 0,
        },
        EUR: {
            totalBalance: 0,
            accountCount: 0,
        },
        USD: {
            totalBalance: 0,
            accountCount: 0,
        },
    };

    summary.forEach((item) => {
        defaultSummary[item.currency] = {
            totalBalance: item.totalBalance ?? 0,
            accountCount: item.accountCount ?? 0,
        };
    });

    return (
        <div>
            <h3>Account Totals</h3>

            <p>
                <strong>RON:</strong> {defaultSummary.RON.totalBalance.toFixed(2)}
                {" "}
                ({defaultSummary.RON.accountCount} accounts)
            </p>

            <p>
                <strong>EUR:</strong> {defaultSummary.EUR.totalBalance.toFixed(2)}
                {" "}
                ({defaultSummary.EUR.accountCount} accounts)
            </p>

            <p>
                <strong>USD:</strong> {defaultSummary.USD.totalBalance.toFixed(2)}
                {" "}
                ({defaultSummary.USD.accountCount} accounts)
            </p>
        </div>
    );
}

export default AccountCurrencySummary;