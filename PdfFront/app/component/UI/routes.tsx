import { 
    MdDashboard, 
    MdCloudUpload, 
    MdHistory, 
    MdLogout
} from "react-icons/md";

interface MenuItem {
    href: string;
    label: string;
    icon: React.ReactNode;
    allowedRoles: string[];
}

const MENU_CONFIG: MenuItem[] = [
    { allowedRoles: ["user"], href: "/inicio", label: "Inicio", icon: <MdDashboard size={24} /> },
    { allowedRoles: ["user", "admin"], href: "/inicio/uploadHC", label: "Subir HC", icon: <MdCloudUpload size={24} /> },
    { allowedRoles: ["user", "admin"], href: "/inicio/readHC", label: "Historias", icon: <MdHistory size={24} /> },

    { allowedRoles: ["admin"], href: "/dashboard", label: "Dashboard", icon: <MdDashboard size={24} /> },
    { allowedRoles: ["admin"], href: "/dashboard/gestion-users", label: "Gestión de usuarios", icon: <MdDashboard size={24} /> },
];

export default MENU_CONFIG;