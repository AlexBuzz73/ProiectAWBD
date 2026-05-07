import { Link, useNavigate } from "react-router-dom";
import { getLoggedUser, removeLoggedUser } from "../../utils/authStorage.js";

function UserDashboardPage() {
    const navigate = useNavigate();
    const user = getLoggedUser();
    const handleLogout = () => {
        removeLoggedUser();
        navigate("/login");
    };

    return (
        <div>
            <h1>User Dashboard</h1>

            <p>
                Welcome, <strong>{user.username}</strong>
            </p>

            <p>Email: {user.email}</p>

            <hr />

            <h2>Accounts</h2>

            <p>Account cards will be displayed here.</p>

            <hr />

            <h2>Recent Transactions</h2>

            <p>Last 5 transactions will be displayed here.</p>

            <hr />

            <h2>Quick Actions</h2>

            <ul>
                <li>
                    <Link to="/accounts">
                        Create / Manage Bank Accounts
                    </Link>
                </li>

                <li>
                    <Link to="/user-limits">
                        Configure User Limits
                    </Link>
                </li>

                <li>
                    Internal / External Payments (Coming soon)
                </li>

                <li>
                    Currency Exchange (Coming soon)
                </li>

                <li>
                    Categories Management (Coming soon)
                </li>
            </ul>

            <button onClick={handleLogout}>
                Logout
            </button>
        </div>
    );
}

export default UserDashboardPage;