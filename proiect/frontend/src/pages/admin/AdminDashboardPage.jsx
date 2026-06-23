import { Link, useNavigate } from "react-router-dom";
import { getLoggedUser, removeLoggedUser } from "../../utils/authStorage.js";
import { logoutUser } from "../../api/authApi.js";

function AdminDashboardPage() {
    const navigate = useNavigate();
    const user = getLoggedUser();

    const handleLogout = async () => {
        await logoutUser();
        removeLoggedUser();
        navigate("/login");
    };

    if (!user) {
        return <p>User is not logged in.</p>;
    }

    return (
        <div>
            <h1>Admin Dashboard</h1>

            <p>
                Welcome, <strong>{user.username}</strong>
            </p>

            <p>Email: {user.email}</p>

            <hr />

            <h2>Administrative Actions</h2>

            <ul>
                <li>
                    <Link to="/admin/unlock-user">Unlock blocked user</Link>
                </li>
                <li>
                    <Link to="/admin/bank-limits">Configure global bank limits</Link>
                </li>
                <li>
                    <Link to="/admin/shared-account">Create shared account</Link>
                </li>
                <li>
                    <Link to="/admin/revoke-access">Revoke shared account access</Link>
                </li>
            </ul>

            <button onClick={handleLogout}>
                Logout
            </button>
        </div>
    );
}

export default AdminDashboardPage;
