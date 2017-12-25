package sample;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Furusato {

	private static class Syotokuzeiritu {
		private int from;
		private int to;
		private int percentage;
		private int cyouseiGaku;

		public Syotokuzeiritu(int from, int to, int percentage, int cyouseiGaku) {
			this.from = from;
			this.to = to;
			this.percentage = percentage;
			this.cyouseiGaku = cyouseiGaku;
		}

		@Override
		public String toString() {
			return "from: " + from + ", to: " + to + ", percentage: " + percentage + ", cyouseigGaku: " + cyouseiGaku;
		}
	}

	private final static List<Syotokuzeiritu> SYOTOKUZEIRITU_TABLE = new ArrayList<>();
	static {
		SYOTOKUZEIRITU_TABLE.add(new Syotokuzeiritu(0, 1950000, 5, 0));
		SYOTOKUZEIRITU_TABLE.add(new Syotokuzeiritu(1950000, 3300000, 10, 97500));
		SYOTOKUZEIRITU_TABLE.add(new Syotokuzeiritu(3300000, 6950000, 20, 427500));
		SYOTOKUZEIRITU_TABLE.add(new Syotokuzeiritu(6950000, 9000000, 23, 636000));
		SYOTOKUZEIRITU_TABLE.add(new Syotokuzeiritu(9000000, 18000000, 33, 1536000));
		SYOTOKUZEIRITU_TABLE.add(new Syotokuzeiritu(18000000, 40000000, 40, 2796000));
		SYOTOKUZEIRITU_TABLE.add(new Syotokuzeiritu(40000000, 99999999, 45, 4796000));
	}

	private final static int SYOTOKU = 6760000;// 収入 - 経費 -青色申告特別控除
	private final static int SYAHO_TOTAL = 637160;// 国保 + 年金
	private final static int SEIHO_TOTAL = 0;
	private final static int JISHIN_TOTAL = 0;
	private final static int ROAN_KOJO = 0;

	public static void main(String[] args) {
		System.out.println(
				"'ふるさと納税額','課税対象額','所得税','所得税から引ききれなかった税額控除','住民税所得割額(ふるさと納税控除反映前)','所得税からの控除','住民税からの基本控除','住民税からの特例控除','控除額合計','住民税所得割額(ふるさと納税控除反映後)','自己負担'");
		for (int furusatoTax = 101000; furusatoTax < 10000000; furusatoTax += 1000) {
			int kazeiTaisyou = kazeiTaisyou(SYOTOKU, furusatoTax, SYAHO_TOTAL, SEIHO_TOTAL, JISHIN_TOTAL);
			int syotokuZei = syotokuZei(kazeiTaisyou, ROAN_KOJO);
			int overRoanKojo = overRoanKojo(kazeiTaisyou, ROAN_KOJO);
			int jyuminBefore = jyuminzeiSyotokuwarigaku(kazeiTaisyou, overRoanKojo);

			int A = kojoFromSyotokuZei(furusatoTax, SYOTOKU);
			int B = kojoFromJyuminKihon(furusatoTax, SYOTOKU);
			int C = kojoFromJyuminTokurei(furusatoTax, SYOTOKU, jyuminBefore);

			int kojoTotal = A + B + C;
			int jyuminAfter = jyuminBefore - B - C;
			int cost = kojoTotal - furusatoTax;

			StringJoiner j = new StringJoiner(",");
			j.add(format(furusatoTax));

			j.add(format(kazeiTaisyou));
			j.add(format(syotokuZei));
			j.add(format(overRoanKojo));
			j.add(format(jyuminBefore));

			j.add(format(A));
			j.add(format(B));
			j.add(format(C));

			j.add(format(kojoTotal));
			j.add(format(jyuminAfter));
			j.add(format(cost));

			System.out.println(j.toString());

			if (cost < -2000) {
				break;
			}
		}
	}

	private static int kazeiTaisyou(int syotoku, int furusatoTax, int syahoTotal, int seihoTotal, int jishinTotal) {
		// 納税額は所得の40%が上限
		if (syotoku * 0.4 <= furusatoTax) {
			furusatoTax = (int) (syotoku * 0.4);
		}
		// 所得税に対する基礎控除38万円
		int tmp = syotoku - 380000 - (furusatoTax - 2000) - syahoTotal - seihoTotal - jishinTotal;
		// 千円未満の切り落とし
		tmp /= 1000;
		tmp *= 1000;
		return tmp;
	}

	private static int syotokuZeiCore(int kazeiTaisyou, int roanKojo) {
		Syotokuzeiritu sz = getSyotokuZeiritu(kazeiTaisyou);
		int tmp = (int) ((int) ((kazeiTaisyou * sz.percentage / 100) - sz.cyouseiGaku - roanKojo) * 1.021);// 復興特別所得税額を加算(2.1%)
		// 百円未満の切り落とし
		tmp /= 100;
		tmp *= 100;
		return tmp;
	}

	private static int syotokuZei(int kazeiTaisyou, int roanKojo) {
		int tmp = syotokuZeiCore(kazeiTaisyou, roanKojo);
		return tmp > 0 ? tmp : 0;
	}

	private static int overRoanKojo(int kazeiTaisyou, int roanKojo) {
		int tmp = syotokuZeiCore(kazeiTaisyou, roanKojo);
		return 0 > tmp ? -1 * tmp : 0;
	}

	private static int jyuminzeiSyotokuwarigaku(int kazeiTaisyou, int overRoanKojo) {
		kazeiTaisyou += 50000;// 住民税の課税対象は基礎控除33万円なので所得税計算における基礎控除38万円との差を埋めるため5万円プラス
		int tmp = ((int) (kazeiTaisyou * 0.1)) - overRoanKojo;
		return tmp > 0 ? tmp : 0;
	}

	private static int kojoFromSyotokuZei(int furusatoTax, int syotoku) {
		// 納税額は所得の40%が上限
		if (syotoku * 0.4 <= furusatoTax) {
			furusatoTax = (int) (syotoku * 0.4);
		}
		return (furusatoTax - 2000) * getSyotokuZeiritu(syotoku).percentage / 100;
	}

	private static int kojoFromJyuminKihon(int furusatoTax, int syotoku) {
		// 納税額は所得の30%が上限
		if (syotoku * 0.3 <= furusatoTax) {
			furusatoTax = (int) (syotoku * 0.3);
		}
		return (int) ((furusatoTax - 2000) * 0.1);
	}

	private static int kojoFromJyuminTokurei(int furusatoTax, int syotoku, int jyuminzeiSyotokuWarigaku) {
		int result = (int) ((furusatoTax - 2000) * (90 - getSyotokuZeiritu(syotoku).percentage) / 100);
		// 住民税特例控除は住民税所得割額の20%が上限
		if (result >= jyuminzeiSyotokuWarigaku * 0.2) {
			return (int) (jyuminzeiSyotokuWarigaku * 0.2);
		} else {
			return result;
		}
	}

	private static Syotokuzeiritu getSyotokuZeiritu(int syotoku) {
		Syotokuzeiritu result = null;
		for (Syotokuzeiritu s : SYOTOKUZEIRITU_TABLE) {
			if (s.from <= syotoku && syotoku < s.to) {
				result = s;
				break;
			}
		}
		return result;
	}

	private static String format(int value) {
		return "'" + String.format("%,d", value) + "'";
	}

}
