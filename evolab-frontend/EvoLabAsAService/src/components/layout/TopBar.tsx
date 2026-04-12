import { Link, Outlet, useNavigate } from "react-router-dom";
import styles from "./TopBar.module.css";
import { useAuth } from "../../contexts/AuthContext";
import { apiUsers } from "../../pages/auth/data/apiUsers";


export function TopBar() {

    const navigate = useNavigate();
    const { user, reload } = useAuth();

    const handleLogout = async () => {
        await apiUsers.logout();
        reload();
        navigate('/');
    }


    return (
        <nav className={styles.topBarContainer}>
            <div className={styles.logo}>
                <Link to="/" >EvoLab</Link>
            </div>

            <div className={styles.navLinks}>
                <Link to="/">Home</Link>
                <Link to="/projects">Projects</Link>
                <Link to="/credentials">Credentials</Link>
                <Link to="/config">Configuration files</Link>
                <Link to="/contact">Contact</Link>


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


                    <button className={styles.signInBtn} onClick={() => navigate('/register')}>
                        Sign In
                    </button>
                )}

            </div>
        </nav>

    )



}    