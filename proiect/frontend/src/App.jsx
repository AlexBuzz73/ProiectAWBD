import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import Navbar from "./components/common/Navbar.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import RegisterPage from "./pages/RegisterPage.jsx";
import UserDashboardPage from "./pages/user/UserDashboardPage.jsx";
import AdminDashboardPage from "./pages/admin/AdminDashboardPage.jsx";
import NotFoundPage from "./pages/NotFoundPage.jsx";
import ProtectedRoute from "./components/auth/ProtectedRoute.jsx";
import NewAccountPage from "./pages/user/NewAccountPage.jsx";
import SingleAccountCreatePage from "./pages/user/SingleAccountCreatePage.jsx";
import AccountDetailsPage from "./pages/user/AccountDetailsPage.jsx";
import CategoriesPage from "./pages/user/CategoriesPage.jsx";
import UserLimitsPage from "./pages/user/UserLimitsPage.jsx";
import NewPaymentPage from "./pages/user/NewPaymentPage.jsx";
import TransferPage from "./pages/user/TransferPage.jsx";
import ExchangePage from "./pages/user/ExchangePage.jsx";
import BankLimitsPage from "./pages/BankLimitsPage.jsx";
import UnlockUserPage from "./pages/admin/UnlockUserPage.jsx";
import CreateSharedAccountPage from "./pages/admin/CreateSharedAccountPage.jsx";
import RevokeAccessPage from "./pages/admin/RevokeAccessPage.jsx";

function App() {
    return (
        <>
            <Navbar />
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
            <Route path="/accounts/new"
                element={
                    <ProtectedRoute requiredRole="USER">
                        <NewAccountPage />
                    </ProtectedRoute>
                }
            />
            <Route path="/accounts/new/single"
                element={
                    <ProtectedRoute requiredRole="USER">
                        <SingleAccountCreatePage />
                    </ProtectedRoute>
                }
            />
            <Route path="/accounts/:accountId"
                element={
                    <ProtectedRoute requiredRole="USER">
                        <AccountDetailsPage />
                    </ProtectedRoute>
                }
            />
            <Route path="/categories"
                element={
                    <ProtectedRoute requiredRole="USER">
                        <CategoriesPage />
                    </ProtectedRoute>
                }
            />
            <Route path="/user-limits"
                   element={
                       <ProtectedRoute requiredRole="USER">
                           <UserLimitsPage />
                       </ProtectedRoute>
                   }
            />
            <Route path="/payments/new"
                   element={
                       <ProtectedRoute requiredRole="USER">
                           <NewPaymentPage />
                       </ProtectedRoute>
                   }
            />
            <Route path="/transfer"
                   element={
                       <ProtectedRoute requiredRole="USER">
                           <TransferPage />
                       </ProtectedRoute>
                   }
            />
            <Route path="/exchange"
                   element={
                       <ProtectedRoute requiredRole="USER">
                           <ExchangePage />
                       </ProtectedRoute>
                   }
            />
            <Route path="/admin/unlock-user"
                   element={
                       <ProtectedRoute requiredRole="ADMIN">
                           <UnlockUserPage />
                       </ProtectedRoute>
                   }
            />
            <Route path="/admin/bank-limits"
                   element={
                       <ProtectedRoute requiredRole="ADMIN">
                           <BankLimitsPage />
                       </ProtectedRoute>
                   }
            />
            <Route path="/admin/shared-account"
                   element={
                       <ProtectedRoute requiredRole="ADMIN">
                           <CreateSharedAccountPage />
                       </ProtectedRoute>
                   }
            />
            <Route path="/admin/revoke-access"
                   element={
                       <ProtectedRoute requiredRole="ADMIN">
                           <RevokeAccessPage />
                       </ProtectedRoute>
                   }
            />
            <Route path="*" element={<NotFoundPage />} />
        </Routes>
        </>
    );
}

export default App;