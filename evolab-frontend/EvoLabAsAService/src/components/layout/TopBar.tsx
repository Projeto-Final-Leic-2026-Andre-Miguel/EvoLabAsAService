import { useEffect, useRef, useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import styles from "./TopBar.module.css";
import { useAuth } from "../../contexts/AuthContext";
import { apiUsers } from "../../pages/Auth/data/apiUsers";
import { clearAuthCookies } from "../../utils/authCookies";

export function TopBar() {
    const navigate = useNavigate();
    const { user, reload } = useAuth();
    const [isProfileOpen, setIsProfileOpen] = useState(false);
    const profileRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (!isProfileOpen) return;

        const handleOutsideClick = (event: MouseEvent) => {
            if (!profileRef.current?.contains(event.target as Node)) {
                setIsProfileOpen(false);
            }
        };
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") setIsProfileOpen(false);
        };

        document.addEventListener("mousedown", handleOutsideClick);
        document.addEventListener("keydown", handleKeyDown);
        return () => {
            document.removeEventListener("mousedown", handleOutsideClick);
            document.removeEventListener("keydown", handleKeyDown);
        };
    }, [isProfileOpen]);

    const handleLogout = async () => {
        setIsProfileOpen(false);
        await apiUsers.logout();
        clearAuthCookies();
        reload();
        navigate('/');
    };

    const handleContactClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
        event.preventDefault();
        setIsProfileOpen(false);
        navigate('/#contact');
        if (window.location.pathname === '/') {
            document.getElementById('contact')?.scrollIntoView({ behavior: 'smooth' });
        }
    };

    return (
        <nav className={styles.topBarContainer}>
            <div className={styles.logo}>
                <Link to="/">EvoLab</Link>
            </div>

            <div className={styles.navLinks}>
                <NavLink to="/" end className={({ isActive }) => isActive ? styles.activeNavLink : undefined}>Home</NavLink>
                <NavLink to="/projects" className={({ isActive }) => isActive ? styles.activeNavLink : undefined}>Projects</NavLink>
                <NavLink to="/credentials" className={({ isActive }) => isActive ? styles.activeNavLink : undefined}>Credentials</NavLink>
                <NavLink to="/config" className={({ isActive }) => isActive ? styles.activeNavLink : undefined}>Configuration</NavLink>
                <a href="#contact" onClick={handleContactClick}>Contact</a>

                {user ? (
                    <div className={styles.userProfileContainer} ref={profileRef}>
                        <button
                            type="button"
                            className={styles.welcomeText}
                            aria-expanded={isProfileOpen}
                            aria-haspopup="menu"
                            onClick={() => setIsProfileOpen(open => !open)}
                        >
                            Welcome {user.name}
                        </button>
                        <div className={`${styles.dropdownMenu} ${isProfileOpen ? styles.dropdownMenuOpen : ''}`} role="menu">
                            <button
                                onClick={() => {
                                    setIsProfileOpen(false);
                                    navigate('/profile');
                                }}
                                className={styles.profileBtn}
                                role="menuitem"
                            >
                                Profile
                            </button>
                            <button onClick={handleLogout} className={styles.logoutBtn} role="menuitem">
                                Logout
                            </button>
                        </div>
                    </div>
                ) : (
                    <button className={styles.signInBtn} onClick={() => navigate('/login')}>
                        Sign In
                    </button>
                )}
            </div>
        </nav>
    );
}
