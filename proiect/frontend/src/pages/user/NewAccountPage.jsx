import { Link } from "react-router-dom";

function NewAccountPage() {
    return (
        <div>
            <h1>Create New Account</h1>

            <p>
                Choose the type of account you want to create.
            </p>

            <hr />

            <h2>Single Account</h2>

            <p>
                Create a personal bank account owned by you.
            </p>

            <Link to="/accounts/new/single">
                Create Single Account
            </Link>

            <hr />

            <h2>Shared / Multiaccount</h2>

            <p>
                Shared account creation is currently available only through the bank.
            </p>

            <hr />

            <Link to="/dashboard">
                Back to Dashboard
            </Link>
        </div>
    );
}

export default NewAccountPage;