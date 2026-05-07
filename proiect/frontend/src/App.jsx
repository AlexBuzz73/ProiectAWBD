import { Navigate, Route, Routes } from "react-router-dom";
import LoginPage from "./pages/LoginPage.jsx";
import RegisterPage from "./pages/RegisterPage.jsx";
import UserDashboardPage from "./pages/user/UserDashboardPage.jsx";
import AdminDashboardPage from "./pages/admin/AdminDashboardPage.jsx";
import NotFoundPage from "./pages/NotFoundPage.jsx";
import ProtectedRoute from "./components/auth/ProtectedRoute.jsx";

function App() {
    return (
        <Routes>
            <Route path="/" element={<Navigate to="/login" replace />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/dashboard"
                element={
                    <ProtectedRoute requiredRole="USER">
                        <UserDashboardPage />
                    </ProtectedRoute>
                }
            />
            <Route path="/admin/dashboard"
                element={
                    <ProtectedRoute requiredRole="ADMIN">
                        <AdminDashboardPage />
                    </ProtectedRoute>
                }
            />
            <Route path="*" element={<NotFoundPage />} />
        </Routes>
    );
}

export default App;