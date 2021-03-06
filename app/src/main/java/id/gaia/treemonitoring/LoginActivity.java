package id.gaia.treemonitoring;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import id.gaia.treemonitoring.database.TBPetani;
import id.gaia.treemonitoring.helper.Connection_Detector;
import id.gaia.treemonitoring.helper.Session;
import id.gaia.treemonitoring.model.GetPetani;
import id.gaia.treemonitoring.model.Petani;
import id.gaia.treemonitoring.rest.ApiClient;
import id.gaia.treemonitoring.rest.ApiInterface;
import me.anwarshahriar.calligrapher.Calligrapher;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btLogin;
    private EditText etUname, etPass;
    private LinearLayout liLayLogin;
    private TBPetani tbPetani;
    private Session session;
    private List<Petani> petaniList = new ArrayList<>();

    ApiInterface pApiInterface;
    Connection_Detector connection_detector;

    //fitur untuk nonaktifkan back diperangkat
    private long backPressedTime;
    private Toast backToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // SET FONT
        Calligrapher calligrapher = new Calligrapher(this);
        calligrapher.setFont(LoginActivity.this, "VarelaRound-Regular.ttf", true);

        // CETAK BLUPRINT DBHANDLER
        tbPetani = new TBPetani(this);

        // INITIALISASI
        liLayLogin = (LinearLayout) findViewById(R.id.liLayLogin);
        session = new Session(this);
        etUname = (EditText) findViewById(R.id.etUname);
        etPass = (EditText) findViewById(R.id.etPass);
        btLogin = (Button) findViewById(R.id.btnLogin);
        btLogin.setOnClickListener(this);

        // MENAMPILKAN STATUS UPDATE PETANI BERDASARKAN STATUS INTERNET
        connection_detector = new Connection_Detector(this);
        if(!connection_detector.isConnected()){
            Snackbar.make(liLayLogin, "Perangkat tidak memiliki koneksi internet. Gagal menyelaraskan database petani terbaru. ", Snackbar.LENGTH_LONG).show();
        }

        if(session.loggedin()) { // JIKA SESSION LOGIN ARAHKAN KE BERANDA
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();
        } else { // JIKA SESSION = LOGOUT, MAKA PROSES DATA API, ITUPUN KALO KONEK INTERNET :)
            if(connection_detector.isConnected()) { // JIKA ADA KONEKSI INTERNET
                // PANGGIL API INTERFACE,
                pApiInterface = ApiClient.getClient().create(ApiInterface.class);
                ProsesPetani();
            } else {
                Snackbar.make(liLayLogin, "Koneksi internet tidak tersedia. Gagal menyelaraskan database. ", Snackbar.LENGTH_LONG).show();
            }
        }

    }

    public void ProsesPetani(){
        Call<GetPetani> petaniCall = pApiInterface.getPetani();
        petaniCall.enqueue(new Callback<GetPetani>() {
            @Override
            public void onResponse(Call<GetPetani> call, Response<GetPetani> response) {
                List<Petani> PetaniList = response.body().getListDataPetani();
                //Log.d("Retrofit Get", "Jumlah Data Petani :" + String.valueOf(PetaniList.size()));

                int PL = PetaniList.size();
                for(int pi = 0; pi < PL; pi=pi+1){
                    if(!tbPetani.cekPetani(String.valueOf(PetaniList.get(pi).getPetani_id()))){ // PERIKSA PETANI ID

                        int petaniId = PetaniList.get(pi).getPetani_id();
                        String petaniName = PetaniList.get(pi).getPetani_name();
                        String petaniGender = PetaniList.get(pi).getPetani_gender();
                        String petaniUname = PetaniList.get(pi).getPetani_uname();
                        String petaniPassword = PetaniList.get(pi).getPetani_password();
                        tbPetani.tambahPetani(new Petani(petaniId, petaniName, petaniGender, petaniUname, petaniPassword));
                        Log.d("Insert SQLITE ", "Nama Petani " + PetaniList.get(pi).getPetani_name());
                    } else {
                        Log.d("Sudah ada ", "Nama Petani " + PetaniList.get(pi).getPetani_name());
                    }


                }
            }

            @Override
            public void onFailure(Call<GetPetani> call, Throwable t) {
                //Log.e("Retrofit Get", t.toString());
                String msg =  t.getMessage();
                Snackbar.make(liLayLogin, msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.btnLogin:
                if(etUname.getText() == null){
                    Snackbar.make(liLayLogin, "Silahkan masukkan username", Snackbar.LENGTH_LONG).show();
                } else {
                    if(etPass.getText() == null){
                        Snackbar.make(liLayLogin, "Silahkan masukkan password", Snackbar.LENGTH_LONG).show();
                    } else {
                        login();
                    }
                }
                break;
            default:

        }
    }

    private void login(){
        String petUname = etUname.getText().toString().trim();
        String petPass = etPass.getText().toString().trim();

        if(tbPetani.cekLoginPetani(petUname, petPass)){ // BERHASIL LOGIN
            session.setLoggedin(true);
            session.setPetaniUname(petUname);
            Intent petaniIntent = new Intent(LoginActivity.this, HomeActivity.class);
            petaniIntent.putExtra("UNAME", petUname.trim());
            kosongkanInputEditText();
            startActivity(petaniIntent);
            finish();
        } else { // GAGAL LOGIN, EROR USERNAME DAN PASSWORD
            Snackbar.make(liLayLogin, getString(R.string.pesan_eror_login), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void kosongkanInputEditText(){
        etUname.setText(null);
        etPass.setText(null);
    }

    @Override
    public void onBackPressed() {
        if(backPressedTime + 2000 > System.currentTimeMillis()){
            backToast.cancel();
            Intent exit = new Intent(Intent.ACTION_MAIN);
            exit.addCategory(Intent.CATEGORY_HOME);
            exit.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(exit);
        }else {
            backToast = Toast.makeText(getBaseContext(), "Tekan Sekali lagi untuk keluar", Toast.LENGTH_SHORT);
            backToast.show();
        }
        backPressedTime = System.currentTimeMillis();
    }
}
