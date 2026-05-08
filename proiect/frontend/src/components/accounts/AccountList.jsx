import AccountCard from "./AccountCard.jsx";

function AccountList({ accounts }) {
    if (accounts.length === 0) {
        return (
            <p>
                You do not have any active accounts.
            </p>
        );
    }

    return (
        <div>
            {accounts.map((account) => (
                <AccountCard
                    key={account.accountId}
                    account={account}
                />
            ))}
        </div>
    );
}

export default AccountList;