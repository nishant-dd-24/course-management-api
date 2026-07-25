import Card from "../ui/Card";
import PageHeader from "../ui/PageHeader";
import { Users, BookOpen, UserPlus, Library, ArrowRight, Activity, BookCheck } from "lucide-react";

function StatCard({ title, value, icon, description }) {
    const Icon = icon;
    return (
        <Card className="flex flex-col">
            <div className="flex items-center justify-between">
                <h3 className="text-sm font-medium text-zinc-400">{title}</h3>
                <Icon className="h-4 w-4 text-zinc-500" />
            </div>
            <div className="mt-3">
                <p className="text-3xl font-semibold tracking-tighter text-zinc-50">{value}</p>
                {description && <p className="mt-1 text-xs text-zinc-500">{description}</p>}
            </div>
        </Card>
    );
}

function ActionCard({ title, description, icon, onClick }) {
    const Icon = icon;
    return (
        <Card
            as="button"
            type="button"
            onClick={onClick}
            className="group flex w-full flex-col text-left transition-all duration-200 hover:-translate-y-1 hover:border-zinc-700 hover:bg-zinc-800/50 hover:shadow-lg"
        >
            <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-xl bg-zinc-800/80 text-zinc-400 ring-1 ring-zinc-700/50 transition-colors group-hover:bg-blue-500/10 group-hover:text-blue-400 group-hover:ring-blue-500/20">
                <Icon className="h-5 w-5" />
            </div>
            <h3 className="text-sm font-semibold text-zinc-100 group-hover:text-zinc-50">{title}</h3>
            <p className="mt-1.5 flex-1 text-sm leading-6 text-zinc-400">{description}</p>
            <div className="mt-4 flex items-center text-xs font-medium text-blue-400 opacity-0 transition-opacity group-hover:opacity-100">
                Access <ArrowRight className="ml-1 h-3.5 w-3.5" />
            </div>
        </Card>
    );
}

function actionsByRole(role) {
    if (role === "ADMIN") {
        return [
            {
                title: "Manage Users",
                description: "Review and manage all users in the system.",
                path: "/users",
                icon: Users,
            },
            {
                title: "View Courses",
                description: "Inspect available courses and platform activity.",
                path: "/courses",
                icon: Library,
            },
        ];
    }

    if (role === "INSTRUCTOR") {
        return [
            {
                title: "My Courses",
                description: "Open your courses and manage course details.",
                path: "/courses",
                icon: BookOpen,
            },
            {
                title: "Course Enrollments",
                description: "Track enrollments and activity for your courses.",
                path: "/enrollments",
                icon: Users,
            },
        ];
    }

    return [
        {
            title: "Available Courses",
            description: "Browse open courses and enrollment options.",
            path: "/courses",
            icon: Library,
        },
        {
            title: "My Enrollments",
            description: "View your active and historical enrollments.",
            path: "/enrollments",
            icon: BookCheck,
        },
    ];
}

function getPlaceholderStats(role) {
    if (role === "ADMIN") {
        return [
            { title: "Total Users", value: "2,405", icon: Users, description: "+14% from last month" },
            { title: "Active Courses", value: "142", icon: BookOpen, description: "+4 new this week" },
            { title: "Platform Activity", value: "98.2%", icon: Activity, description: "System health score" },
        ];
    }
    if (role === "INSTRUCTOR") {
        return [
            { title: "My Students", value: "840", icon: Users, description: "Across all active courses" },
            { title: "Published Courses", value: "4", icon: BookOpen, description: "1 draft pending" },
            { title: "Avg. Completion", value: "68%", icon: Activity, description: "+5% from last cohort" },
        ];
    }
    return [
        { title: "Active Enrollments", value: "3", icon: BookCheck, description: "In progress" },
        { title: "Completed Courses", value: "5", icon: BookOpen, description: "Lifetime" },
        { title: "Total Hours", value: "124", icon: Activity, description: "Learning time" },
    ];
}

export default function DashboardContent({ role, onNavigate }) {
    const actions = actionsByRole(role);
    const stats = getPlaceholderStats(role);

    return (
        <main className="mx-auto w-full max-w-6xl space-y-8 px-4 py-8">
            <PageHeader
                eyebrow="Overview"
                title="Dashboard"
                subtitle="Welcome back. Here's what's happening today."
            />

            <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {stats.map((stat) => (
                    <StatCard key={stat.title} {...stat} />
                ))}
            </section>

            <div>
                <h2 className="mb-4 text-lg font-semibold tracking-tight text-zinc-100">Quick Actions</h2>
                <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                    {actions.map((action) => (
                        <ActionCard
                            key={`${action.path}-${action.title}`}
                            title={action.title}
                            description={action.description}
                            icon={action.icon}
                            onClick={() => onNavigate(action.path)}
                        />
                    ))}
                </section>
            </div>
        </main>
    );
}
