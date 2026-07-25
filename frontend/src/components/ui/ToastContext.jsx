import { createContext, useContext, useState, useCallback, useMemo } from "react";
import { CheckCircle2, XCircle, AlertCircle, Info, X } from "lucide-react";
import { cn } from "../../lib/cn";

const ToastContext = createContext(null);

let toastCount = 0;

export function ToastProvider({ children }) {
    const [toasts, setToasts] = useState([]);

    const addToast = useCallback(({ title, description, variant = "info", duration = 5000 }) => {
        const id = ++toastCount;
        const toast = { id, title, description, variant };
        
        setToasts((prev) => [...prev, toast]);

        if (duration > 0) {
            setTimeout(() => {
                setToasts((prev) => prev.filter((t) => t.id !== id));
            }, duration);
        }
    }, []);

    const removeToast = useCallback((id) => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
    }, []);

    const contextValue = useMemo(() => ({
        toast: addToast,
        success: (title, desc, dur) => addToast({ title, description: desc, variant: "success", duration: dur }),
        error: (title, desc, dur) => addToast({ title, description: desc, variant: "error", duration: dur }),
        warning: (title, desc, dur) => addToast({ title, description: desc, variant: "warning", duration: dur }),
        info: (title, desc, dur) => addToast({ title, description: desc, variant: "info", duration: dur }),
    }), [addToast]);

    return (
        <ToastContext.Provider value={contextValue}>
            {children}
            <div className="fixed bottom-0 right-0 z-50 flex max-h-screen w-full flex-col-reverse p-4 sm:bottom-4 sm:right-4 sm:w-96 sm:flex-col gap-3">
                {toasts.map((t) => (
                    <Toast key={t.id} toast={t} onRemove={removeToast} />
                ))}
            </div>
        </ToastContext.Provider>
    );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useToast() {
    const context = useContext(ToastContext);
    if (!context) {
        throw new Error("useToast must be used within a ToastProvider");
    }
    return context;
}

const variantStyles = {
    success: "border-emerald-500/20 bg-emerald-500/10 text-emerald-500",
    error: "border-red-500/20 bg-red-500/10 text-red-500",
    warning: "border-amber-500/20 bg-amber-500/10 text-amber-500",
    info: "border-blue-500/20 bg-blue-500/10 text-blue-500",
};

const icons = {
    success: CheckCircle2,
    error: XCircle,
    warning: AlertCircle,
    info: Info,
};

function Toast({ toast, onRemove }) {
    const Icon = icons[toast.variant];
    const style = variantStyles[toast.variant];

    return (
        <div className={cn(
            "pointer-events-auto flex w-full items-start gap-3 rounded-xl border p-4 shadow-lg backdrop-blur-md transition-all animate-toast-in bg-zinc-900/90",
            style
        )}>
            <Icon className="mt-0.5 h-5 w-5 shrink-0" />
            <div className="flex-1 space-y-1">
                {toast.title && <h4 className="text-sm font-semibold text-zinc-100">{toast.title}</h4>}
                {toast.description && <p className="text-sm text-zinc-300">{toast.description}</p>}
            </div>
            <button
                type="button"
                onClick={() => onRemove(toast.id)}
                className="shrink-0 rounded-lg p-1 text-zinc-400 opacity-70 transition-opacity hover:bg-zinc-800 hover:opacity-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400"
            >
                <X className="h-4 w-4" />
            </button>
        </div>
    );
}
