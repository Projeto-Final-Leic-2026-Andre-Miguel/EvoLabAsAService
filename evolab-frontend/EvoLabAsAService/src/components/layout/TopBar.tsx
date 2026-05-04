import { Link, useNavigate } from "react-router-dom";
import styles from "./TopBar.module.css";
import { useAuth } from "../../contexts/AuthContext";
import { apiUsers } from "../../pages/Auth/data/apiUsers";

export function TopBar() {
    const navigate = useNavigate();
    const { user, reload } = useAuth();

    const handleLogout = async () => {
        await apiUsers.logout();
        reload();
        navigate('/');
    }

    const handleContactClick = (e: React.MouseEvent<HTMLAnchorElement>) => {
        e.preventDefault();
        navigate('/#contact');
        // Se já estiver na Home, forçar o scroll
        if (window.location.pathname === '/') {
            const el = document.getElementById('contact');
            if (el) el.scrollIntoView({ behavior: 'smooth' });
        }
    };

    return (
        <nav className={styles.topBarContainer}>
            <div className={styles.logo}>
                <Link to="/">EvoLab</Link>
            </div>

            <div className={styles.navLinks}>
                <Link to="/">Home</Link>
                <Link to="/projects">Projects</Link>
                <Link to="/credentials">Credentials</Link>
                <Link to="/config">Configuration</Link>
                <a href="#contact" onClick={handleContactClick}>Contact</a>

                {user ? (
                    <div className={styles.userProfileContainer}>
                        <div className={styles.welcomeText}>
                            Welcome {user.name}
                        </div>
                        <div className={styles.dropdownMenu}>
                            <button onClick={() => navigate('/profile')} className={styles.profileBtn}>
                                Profile
                            </button>
                            <button onClick={handleLogout} className={styles.logoutBtn}>
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

    )



}    
