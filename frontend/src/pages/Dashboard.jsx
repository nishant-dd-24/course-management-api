import DashboardContent from "../components/dashboard/DashboardContent";
import DashboardHeader from "../components/dashboard/DashboardHeader";

export default function Dashboard({ user, onLogout, onNavigate }) {
    return (
        <div className="min-h-screen bg-slate-950 text-slate-100">
            <DashboardHeader user={user} onLogout={onLogout} onNavigate={onNavigate} />
            <DashboardContent role={user?.role} onNavigate={onNavigate} />
        </div>
    );
}

