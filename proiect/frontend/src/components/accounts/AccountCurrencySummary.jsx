function AccountCurrencySummary({ accounts }) {
    const totals = accounts.reduce(
        (acc, account) => {
            acc[account.currency] += account.balance;
            return acc;
        },
        {
            RON: 0,
            EUR: 0,
            USD: 0,
        }
    );

    return (
        <div>
            <h3>Account Totals</h3>

            <p>
                <strong>RON:</strong> {totals.RON}
            </p>

            <p>
                <strong>EUR:</strong> {totals.EUR}
            </p>

            <p>
                <strong>USD:</strong> {totals.USD}
            </p>
        </div>
    );
}

export default AccountCurrencySummary;